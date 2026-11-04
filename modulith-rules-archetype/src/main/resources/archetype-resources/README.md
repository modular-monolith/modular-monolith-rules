# ${artifactId}

A small modular monolith with two modules, Ordering and Payments, and a
Modulith Rules architecture test already wired in.

```bash
mvn test
```

The test passes as generated. To see a violation, make a class under
`ordering/internal` import `payments.internal.PaymentLedger` and run the
tests again. The failure names the class, the package it reached into, and
suggests the fix.

Module layout:

```
${package}
├── ordering
│   ├── api        OrderService, what other modules may call
│   └── internal   OrderServiceImpl, nobody else's business
└── payments
    ├── api        PaymentService
    └── internal   PaymentLedger
```

Add a module by creating a package under `${package}` and adding its name to
the list in `ArchitectureTest`.
