package app.tastile.android.data.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCacheMapper
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.data.timeline.local.TimelineDayMembershipEntity
import app.tastile.android.data.tile.TileRepository
import app.tastile.android.data.tile.TimelineFetchResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Fetches finite ranges from the canonical TileRepository and commits one
 * normalized cache transaction after every range has validated successfully.
 * Existing rows remain untouched when the network or payload is invalid.
 */
@Singleton
class DefaultTimelineSyncRepository @Inject constructor(
    private val tileRepository: TileRepository,
    private val dao: TimelineCacheDao,
) : TimelineSyncRepository {
    private val generationCounter = AtomicLong(0L)
    private val inFlight = mutableListOf<InFlightRequest>()
    private val inFlightFetches = mutableListOf<InFlightFetch>()
    private val inFlightLock = Any()

    override suspend fun refresh(request: TimelineRefreshRequest): TimelineRefreshResult {
        val requestedDates = request.requestedDates
        val context = RefreshContext.from(request)
        return coroutineScope {
            val existing = synchronized(inFlightLock) {
                inFlight.firstOrNull { candidate ->
                    candidate.context == context && requestedDates.all { it in candidate.requestedDates }
                }
            }
            val deferred = existing?.deferred ?: synchronized(inFlightLock) {
                inFlight.firstOrNull { candidate ->
                    candidate.context == context && requestedDates.all { it in candidate.requestedDates }
                }?.deferred ?: async(start = CoroutineStart.LAZY) {
                    val generation = request.generation ?: generationCounter.incrementAndGet()
                    if (request.generation != null) {
                        generationCounter.updateAndGet { current -> maxOf(current, generation) }
                    }
                    performRefresh(request, generation)
                }.also { deferred ->
                    inFlight += InFlightRequest(
                        context = context,
                        requestedDates = requestedDates,
                        deferred = deferred,
                    )
                }
            }
            try {
                deferred.await()
            } finally {
                synchronized(inFlightLock) {
                    inFlight.removeAll { it.deferred === deferred }
                }
            }
        }
    }

    private suspend fun performRefresh(
        request: TimelineRefreshRequest,
        generation: Long,
    ): TimelineRefreshResult {
        val key = request.normalizedKey
        val requestedDates = request.requestedDates
        if (requestedDates.isEmpty()) {
            return TimelineRefreshResult(
                status = TimelineRefreshStatus.Skipped,
                generation = generation,
            )
        }

        val coverage = dao.observeCoverage(
            accountId = key.accountId,
            scopeKey = key.scopeKey,
            zoneId = key.zoneId.id,
            localDates = requestedDates.map(LocalDate::toString),
        ).first()
        val coverageByDate = coverage.associateBy { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
        val candidateDates = requestedDates.filter { date ->
            val row = coverageByDate[date]
            row == null || row.lastFailureKind != null ||
                row.fetchedAtEpochMs < request.now.toEpochMilli() - request.staleAfter.toMillis()
        }
        if (candidateDates.isEmpty()) {
            return TimelineRefreshResult(
                status = TimelineRefreshStatus.Skipped,
                generation = generation,
            )
        }

        val ranges = mergeAdjacentRanges(candidateDates, key.zoneId)
        val fetchContext = FetchContext.from(request)
        val parsedResponses = mutableListOf<ParsedTimelineItem>()
        for (range in ranges) {
            val fetched = try {
                fetchRange(fetchContext, range, key.zoneId)
            } catch (error: Exception) {
                // Cancellation must propagate so a caller can stop a request;
                // ordinary API/network failures are persisted as metadata.
                if (error is kotlinx.coroutines.CancellationException) throw error
                val failureRows = range.localDates.map { date ->
                    failureCoverage(key, date, request, generation, "network")
                }
                dao.recordFailures(failureRows)
                return TimelineRefreshResult(
                    status = TimelineRefreshStatus.Failed,
                    failedDates = range.localDates,
                    ranges = ranges,
                    generation = generation,
                    failureMessage = error.message,
                )
            }

            val validated = fetched.map { item ->
                parseItem(item)
                    ?: return TimelineRefreshResult(
                        status = TimelineRefreshStatus.Failed,
                        failedDates = range.localDates,
                        ranges = ranges,
                        generation = generation,
                        failureMessage = "Invalid timeline item ${item.id}",
                    )
            }
            parsedResponses += validated
        }

        // One deterministic item per canonical placement id. The endpoint
        // should not duplicate ids, but duplicate responses can occur when
        // separately merged ranges touch at a boundary.
        val parsedItems = parsedResponses
            .filter { item -> candidateDates.any { date -> overlaps(item, date, key.zoneId) } }
            .distinctBy { it.item.id }
        val itemEntities = parsedItems.map { parsed ->
            TimelineCacheMapper.toEntity(key.accountId, key.scopeKey, parsed.item)
        }
        val memberships = parsedItems.flatMap { parsed ->
            candidateDates.filter { date ->
                overlaps(parsed, date, key.zoneId)
            }.map { date ->
                TimelineDayMembershipEntity(
                    accountId = key.accountId,
                    scopeKey = key.scopeKey,
                    zoneId = key.zoneId.id,
                    localDate = date.toString(),
                    itemId = parsed.item.id,
                )
            }
        }.distinct()
        val fetchedAt = request.now.toEpochMilli()
        val coverageEntities = candidateDates.map { date ->
            TimelineCoverageEntity(
                accountId = key.accountId,
                scopeKey = key.scopeKey,
                zoneId = key.zoneId.id,
                localDate = date.toString(),
                fetchedAtEpochMs = fetchedAt,
                contractVersion = TimelineRefreshRequest.CACHE_CONTRACT_VERSION,
                lastFailureKind = null,
                lastAccessedAtEpochMs = fetchedAt,
                refreshGeneration = generation,
            )
        }

        dao.replaceDays(
            items = itemEntities,
            memberships = memberships,
            coverage = coverageEntities,
        )

        val committedCoverage = dao.observeCoverage(
            accountId = key.accountId,
            scopeKey = key.scopeKey,
            zoneId = key.zoneId.id,
            localDates = candidateDates.map(LocalDate::toString),
        ).first()
        val superseded = committedCoverage.any { row ->
            row.localDate in candidateDates.map(LocalDate::toString) &&
                row.refreshGeneration > generation
        }
        return TimelineRefreshResult(
            status = if (superseded) TimelineRefreshStatus.Superseded else TimelineRefreshStatus.Refreshed,
            refreshedDates = candidateDates,
            ranges = ranges,
            generation = generation,
        )
    }

    /**
     * Claims the uncovered portions of [range] while sharing every already
     * running fetch that intersects it. A request such as [16..22] therefore
     * shares date 22 with a concurrent [22..28] request and only starts a
     * second network range for [23..28].
     */
    private suspend fun fetchRange(
        context: FetchContext,
        range: TimelineUtcRange,
        zone: ZoneId,
    ): List<CoreTimelineItem> = coroutineScope {
        val flights = synchronized(inFlightLock) {
            val shared = inFlightFetches.filter { candidate ->
                candidate.context == context &&
                    candidate.localDates.any { it in range.localDates }
            }
            val coveredDates = shared
                .asSequence()
                .flatMap { it.localDates.asSequence() }
                .toSet()
            val uncoveredDates = range.localDates.filterNot { it in coveredDates }
            val own = mergeAdjacentRanges(uncoveredDates, zone).map { uncoveredRange ->
                InFlightFetch(
                    context = context,
                    localDates = uncoveredRange.localDates.toSet(),
                    deferred = async(start = CoroutineStart.LAZY) {
                        when (val result = tileRepository.getTimelineCanonical(
                            uncoveredRange.start,
                            uncoveredRange.end,
                            context.ownerIds,
                        )) {
                            is TimelineFetchResult.Success -> result.items
                            is TimelineFetchResult.Failure -> throw result.error
                        }
                    },
                )
            }
            inFlightFetches += own
            shared + own
        }

        try {
            // Start all newly claimed ranges before awaiting any shared range;
            // an uncovered tail should not wait behind a slow overlapping call.
            flights.forEach { it.deferred.start() }
            flights.flatMap { it.deferred.await() }
        } finally {
            // A waiter must not remove an unfinished flight owned by another
            // refresh. Completed entries are safe to remove and will be
            // removed by whichever waiter observes completion first.
            synchronized(inFlightLock) {
                inFlightFetches.removeAll { candidate ->
                    candidate in flights && candidate.deferred.isCompleted
                }
            }
        }
    }

    private fun parseItem(item: CoreTimelineItem): ParsedTimelineItem? {
        if (item.id.isBlank()) return null
        val start = parseInstant(item.startAt) ?: return null
        val end = if (item.endAt == null) {
            null
        } else {
            parseInstant(item.endAt) ?: return null
        }
        if (end != null && !end.isAfter(start)) return null
        return ParsedTimelineItem(item, start, end)
    }

    private fun overlaps(
        item: ParsedTimelineItem,
        date: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        return item.start < dayEnd && (item.end == null || item.end > dayStart)
    }

    private fun mergeAdjacentRanges(
        dates: List<LocalDate>,
        zone: ZoneId,
    ): List<TimelineUtcRange> {
        if (dates.isEmpty()) return emptyList()
        val sorted = dates.distinct().sorted()
        val result = mutableListOf<TimelineUtcRange>()
        var rangeStart = sorted.first()
        var rangeEnd = rangeStart
        for (date in sorted.drop(1)) {
            if (date == rangeEnd.plusDays(1)) {
                rangeEnd = date
            } else {
                result += utcRange(rangeStart, rangeEnd, zone)
                rangeStart = date
                rangeEnd = date
            }
        }
        result += utcRange(rangeStart, rangeEnd, zone)
        return result
    }

    private fun utcRange(
        firstDate: LocalDate,
        lastDate: LocalDate,
        zone: ZoneId,
    ): TimelineUtcRange {
        val start = firstDate.atStartOfDay(zone).toInstant()
        val end = lastDate.plusDays(1).atStartOfDay(zone).toInstant()
        return TimelineUtcRange(
            start = start,
            end = end,
            localDates = generateSequence(firstDate) { current ->
                current.plusDays(1).takeIf { it <= lastDate }
            }.toList(),
        )
    }

    private fun failureCoverage(
        key: TimelinePageKey,
        date: LocalDate,
        request: TimelineRefreshRequest,
        generation: Long,
        failureKind: String,
    ): TimelineCoverageEntity {
        val now = request.now.toEpochMilli()
        return TimelineCoverageEntity(
            accountId = key.accountId,
            scopeKey = key.scopeKey,
            zoneId = key.zoneId.id,
            localDate = date.toString(),
            fetchedAtEpochMs = now,
            contractVersion = TimelineRefreshRequest.CACHE_CONTRACT_VERSION,
            lastFailureKind = failureKind,
            lastAccessedAtEpochMs = now,
            refreshGeneration = generation,
        )
    }

    private fun parseInstant(value: String): Instant? = runCatching {
        Instant.parse(value)
    }.recoverCatching {
        java.time.OffsetDateTime.parse(value).toInstant()
    }.getOrNull()

    private data class ParsedTimelineItem(
        val item: CoreTimelineItem,
        val start: Instant,
        val end: Instant?,
    )

    private data class RefreshContext(
        val accountId: String,
        val scopeKey: String,
        val zoneId: String,
        val ownerIds: List<String>,
        val generation: Long?,
    ) {
        companion object {
            fun from(request: TimelineRefreshRequest): RefreshContext {
                val key = request.normalizedKey
                return RefreshContext(
                    accountId = key.accountId,
                    scopeKey = key.scopeKey,
                    zoneId = key.zoneId.id,
                    ownerIds = request.ownerIds.filter(String::isNotBlank).distinct().sorted(),
                    generation = request.generation,
                )
            }
        }
    }

    private data class FetchContext(
        val accountId: String,
        val scopeKey: String,
        val zoneId: ZoneId,
        val ownerIds: List<String>,
        val generation: Long?,
    ) {
        companion object {
            fun from(request: TimelineRefreshRequest): FetchContext {
                val key = request.normalizedKey
                return FetchContext(
                    accountId = key.accountId,
                    scopeKey = key.scopeKey,
                    zoneId = key.zoneId,
                    ownerIds = request.ownerIds.filter(String::isNotBlank).distinct().sorted(),
                    generation = request.generation,
                )
            }
        }
    }

    private data class InFlightRequest(
        val context: RefreshContext,
        val requestedDates: List<LocalDate>,
        val deferred: Deferred<TimelineRefreshResult>,
    )

    private data class InFlightFetch(
        val context: FetchContext,
        val localDates: Set<LocalDate>,
        val deferred: Deferred<List<CoreTimelineItem>>,
    )
}
