# BFT-SMaRt Counter Demo Runbook (Single Machine)

## 1) Counter demo classes and entrypoints

- Replica: `bftsmart.demo.counter.CounterServer`
- Client: `bftsmart.demo.counter.CounterClient`

Source files:
- `src/main/java/bftsmart/demo/counter/CounterServer.java`
- `src/main/java/bftsmart/demo/counter/CounterClient.java`

## 2) Build and run path

```bash
./gradlew installDist
cd build/install/library
```

All `smartrun.sh` commands below are expected to run from `build/install/library`.

## 3) config/currentView handling

If you changed replica membership/addresses (e.g. `config/hosts.config` or `system.initial.view` in `config/system.config`), remove `config/currentView` before starting replicas.

```bash
rm -f config/currentView
```

Why: when `config/currentView` exists, BFT-SMaRt uses it as the current view. If it does not exist, it is created from configuration at startup.

## 4) Start order (4 replicas + 1 client)

Start replicas first, one process per terminal:

```bash
./smartrun.sh bftsmart.demo.counter.CounterServer 0
./smartrun.sh bftsmart.demo.counter.CounterServer 1
./smartrun.sh bftsmart.demo.counter.CounterServer 2
./smartrun.sh bftsmart.demo.counter.CounterServer 3
```

Wait until each replica prints:

```text
Ready to process operations
```

Then start client:

```bash
./smartrun.sh bftsmart.demo.counter.CounterClient 1001 1 10
```

## 5) Helper script

You can automate local run from repository root:

```bash
runscripts/run-counter-local.sh 1001 1 10
```

Optional: skip build if `build/install/library` already exists.

```bash
SKIP_BUILD=1 runscripts/run-counter-local.sh 1001 1 10
```
