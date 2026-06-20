# Security

## Sensitive Material

- Release keystores, keystore passwords, and machine-local Gradle settings must stay out of this repository.
- Publishable Cognito client values may exist in source control, but server secrets must not.

## Reporting

If you discover a security issue in this repository or in the mobile release process, report it privately to the project maintainers before opening a public issue.

## Operational Expectations

- Rotate any upload key immediately if it was ever committed or shared improperly.
- Treat cloned working directories as disposable. Re-clone rather than carrying forward unknown local state.
