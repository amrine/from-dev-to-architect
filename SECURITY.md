# Security Policy

Thank you for helping keep TeamPulse and this learning repository secure.

## Supported Versions

This project is under active development and does not currently publish stable
releases. Security fixes are applied to the latest version of `main`.

| Version                    | Supported                           |
| -------------------------- | ----------------------------------- |
| Latest `main`              | Yes                                 |
| Older commits and branches | No                                  |
| Published releases         | No releases are currently published |

Backports are not currently provided. If a stable release process is introduced,
this table will be updated with explicit support periods.

## Reporting a Vulnerability

Do not report suspected vulnerabilities through a public GitHub issue,
discussion, or pull request.

Send reports privately to
[amrinemoussab@gmail.com](mailto:amrinemoussab@gmail.com) with the subject
`[from-dev-to-architect Security]`.

Include as much of the following information as possible:

- the affected module, file, dependency, workflow, or configuration;
- the affected commit or version;
- a clear description of the vulnerability and its potential impact;
- reproducible steps or a minimal proof of concept;
- any prerequisites, logs, or relevant environment details;
- a suggested remediation, if available;
- whether and how you would like to be credited.

Do not include credentials, personal data, or data obtained from systems you do
not own or have explicit permission to test.

## Response Process

The maintainer aims to:

1. acknowledge a report within five business days;
2. perform an initial assessment and request any missing information;
3. confirm whether the report is accepted, requires further investigation, or
   is outside the scope of this policy;
4. coordinate remediation and disclosure with the reporter when the issue is
   accepted;
5. publish a security advisory when disclosure would help downstream users.

Response and remediation times depend on severity, complexity, and maintainer
availability. This is a volunteer-maintained learning project, so these targets
are goals rather than guaranteed service levels.

## Coordinated Disclosure

Please keep vulnerability details confidential until a fix or mitigation is
available and a disclosure timeline has been agreed upon. Avoid exploiting a
vulnerability beyond what is necessary to demonstrate it, accessing other
people's data, disrupting services, or degrading repository availability.

The maintainer will make a good-faith effort to communicate progress and credit
reporters who request attribution.

## Scope

Reports may cover:

- TeamPulse application code and configuration;
- build, test, and local infrastructure scripts;
- GitHub Actions workflows and repository automation;
- dependencies when their use creates a demonstrable risk in this repository;
- documentation that instructs users to adopt an unsafe configuration.

The following are normally outside scope:

- vulnerabilities in an upstream dependency without a demonstrated impact on
  this repository;
- automated scanner output without a reproducible security impact;
- social engineering, phishing, spam, or denial-of-service testing;
- reports about unsupported branches or historical commits;
- requests for a bug bounty or financial reward.

This project does not currently operate a paid bug bounty program.
