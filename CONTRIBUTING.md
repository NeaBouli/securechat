# Contributing to SecureChat

Thank you for your interest in SecureChat. We value transparency, security review, and responsible vulnerability reports.

## What We Accept

- Bug reports via [GitHub Issues](https://github.com/NeaBouli/securechat/issues)
- Feature requests via [GitHub Issues](https://github.com/NeaBouli/securechat/issues)
- Security vulnerability reports via the process in [SECURITY.md](SECURITY.md)

## What We Do Not Accept

We do not accept code contributions or pull requests.

This repository is source-available for transparency and independent security auditing only. Forks, builds, derivative works, redistribution, rebranding, hosting, and any use of SecureChat or official StealthX services require prior written permission from Vendetta Labs.

- Pull requests will be closed without review.
- Patches, code suggestions, or implementation changes submitted via issues or other channels will not be incorporated.

### Security-Critical Changes

Changes to the following areas require **additional review** from a security-focused maintainer:

- Cryptographic implementations (XChaCha20, X25519, Double Ratchet, Argon2id, Ed25519)
- Android Keystore integration
- Key management and storage
- Transport layer and relay protocol
- Any code handling private keys or plaintext messages

## Guidelines

- Keep PRs focused — one feature or fix per PR.
- Write clear commit messages that explain **why**, not just **what**.
- Add tests for new functionality where applicable.
- Follow existing code style and conventions.
- Update documentation if your changes affect public APIs or user-facing behavior.

## License

SecureChat is licensed under the StealthX Source-Available License. You may read and inspect the source code for transparency and security review, but you may not copy, modify, build, run, distribute, rebrand, host, or use SecureChat without prior written permission from Vendetta Labs.

## Reporting Bugs

- Use [GitHub Issues](https://github.com/NeaBouli/securechat/issues) for bug reports and feature requests.
- For **security vulnerabilities**, see [SECURITY.md](SECURITY.md) — do not open a public issue.

## Communication

- GitHub Issues for feature discussions and bug reports
- Pull Request comments for code-specific discussions
- kaspartisan@proton.me for private matters
## Gradle dependency verification

Dependencies are checksum-locked in `gradle/verification-metadata.xml`. When a reviewed
dependency update changes the graph, rerun the same affected Gradle CI tasks with
`--write-verification-metadata sha256`, inspect the metadata diff for only the expected
component/version changes, and then rerun the tasks without the write flag. Do not accept
unrelated checksum churn.
