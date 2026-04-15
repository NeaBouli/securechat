# Contributing to SecureChat

Thank you for your interest in contributing to SecureChat. This document outlines the process for contributing to the project.

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```
   git clone https://github.com/YOUR-USERNAME/securechat.git
   ```
3. **Create a branch** for your changes:
   ```
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** and commit them with clear, descriptive messages
5. **Push** your branch to your fork:
   ```
   git push origin feature/your-feature-name
   ```
6. **Open a Pull Request** against the `main` branch

## Code Review

- **All pull requests require code review** before merging.
- At least one maintainer must approve the PR.
- CI checks must pass before merging.

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

SecureChat is licensed under **GPL-3.0**. By submitting a pull request, you agree that your contributions will be licensed under the same license.

- All contributions must be compatible with GPL-3.0.
- **No proprietary dependencies allowed.** All dependencies must be open-source with a GPL-compatible license.
- If you are unsure whether a dependency is compatible, ask in the PR discussion.

## Reporting Bugs

- Use [GitHub Issues](https://github.com/NeaBouli/securechat/issues) for bug reports and feature requests.
- For **security vulnerabilities**, see [SECURITY.md](SECURITY.md) — do not open a public issue.

## Communication

- GitHub Issues for feature discussions and bug reports
- Pull Request comments for code-specific discussions
- kaspartisan@proton.me for private matters
