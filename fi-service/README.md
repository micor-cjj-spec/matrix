# FI Service

This module consolidates several financial-related services into a single codebase.
The following applications are now organised under `fi-service`:

- Accounts Receivable (`ar`)
- Accounts Payable (`ap`)
- Payroll (`payroll`)
- General Ledger (`gl`)
- Fund Management (`fund`)
- Cashier (`cashier`)
- Asset Management (`asset`)

Each application has its own main class under `single.cjj.fi.<app>` package so
that they can still be started independently.
