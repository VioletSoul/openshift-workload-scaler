![OpenShift](https://img.shields.io/badge/OpenShift-Kubernetes-red)
![Jenkins](https://img.shields.io/badge/Jenkins-Pipeline-blue)
![Groovy](https://img.shields.io/badge/Language-Groovy-green)
![Automation](https://img.shields.io/badge/Automation-Workload%20Management-success)
![CI/CD](https://img.shields.io/badge/Focus-OpenShift%20Operations-orange)

![Repo Size](https://img.shields.io/github/repo-size/VioletSoul/openshift-workload-scaler)
![Code Size](https://img.shields.io/github/languages/code-size/VioletSoul/openshift-workload-scaler)
[![Stars](https://img.shields.io/github/stars/VioletSoul/openshift-workload-scaler.svg?style=social)](https://github.com/VioletSoul/openshift-workload-scaler)
[![Last Commit](https://img.shields.io/github/last-commit/VioletSoul/openshift-workload-scaler.svg)](https://github.com/VioletSoul/openshift-workload-scaler/commits/main)

---

# Overview

This repository contains a Jenkins Pipeline designed to automate controlled startup and shutdown of OpenShift workloads.

Unlike a simple `oc scale` operation, the pipeline preserves deployment state, supports dry-run execution, restores the original replica count, and starts workloads in dependency-aware stages to reduce startup failures.

The project was originally created for production OpenShift environments where dozens of deployments must be stopped and restored safely with minimal operator intervention.

---

# Architecture

The pipeline performs the following high-level workflow:

1. Authenticate to OpenShift.
2. Select target project.
3. Read current deployment state.
4. Save current replica count.
5. Scale workloads down in a predefined order.
6. Restore workloads using the previously saved replica count.
7. Archive the deployment snapshot.

---

# Pipeline parameters

The pipeline exposes several runtime parameters.

| Parameter | Description |
|-----------|-------------|
| environment | Target OpenShift environment |
| action | Stop or Start workloads |
| dryRun | Preview operations without applying changes |
| username | OpenShift username |
| password | OpenShift password |

---

# Shutdown workflow

Before shutting down workloads the pipeline:

- logs into OpenShift
- switches to the target project
- collects current replica counts
- stores deployment state
- archives the snapshot

Deployments are then stopped in two phases.

## Phase 1

Infrastructure-related deployments.

Typical examples include:

- scheduler
- timeout services

These are stopped first.

Afterwards the pipeline waits for 60 seconds before continuing.

This delay allows scheduled jobs to terminate gracefully before application workloads disappear.

---

## Phase 2

All remaining deployments are scaled down.

The original replica count of every deployment is preserved inside a snapshot file.

---

# Startup workflow

During restoration the pipeline first attempts to restore the exact deployment state that existed before shutdown.

If no snapshot is available, every deployment is started with a single replica.

Instead of starting everything simultaneously, deployments are divided into logical groups.

---

## Group 1

Ingress / Egress components

These workloads expose networking functionality and therefore must become available before business services.

---

## Group 2

Infrastructure services

Examples include:

- dataspace
- monitoring
- internal shared services

---

## Group 3

Application workloads

Business services are started only after the infrastructure layer becomes available.

---

# Replica state preservation

One of the main design goals is preserving the original deployment configuration.

Before scaling down, the pipeline records:

- deployment name
- configured replica count

The information is written into:

```

replicas_state.txt

```

During startup this snapshot is used to restore every deployment back to its previous size.

---

# Dry-run mode

The pipeline supports simulation mode.

When enabled it:

- does not modify OpenShift
- prints every action
- shows the current deployment state
- displays which deployments would be scaled
- validates the execution logic

This allows operators to verify the execution plan before performing changes.

---

# Deployment grouping strategy

The pipeline intentionally avoids starting every deployment simultaneously.

Benefits include:

- lower API load
- reduced CPU spikes
- controlled dependency initialization
- lower probability of startup failures
- predictable recovery sequence

This strategy proved significantly more reliable than parallel startup in large OpenShift projects.

---

# Artifact generation

Every execution archives the deployment snapshot.

Artifacts contain:

- deployment names
- original replica count

This allows:

- auditing
- troubleshooting
- recovery validation

---

# Error handling

The pipeline validates:

- selected environment
- project availability
- OpenShift authentication
- deployment readiness
- saved replica state

Invalid configuration immediately terminates execution with a descriptive error message.

---

# Design principles

The implementation follows several operational principles.

- Stateless execution
- Repeatable operations
- Safe rollback
- Minimal operator interaction
- Environment abstraction
- Dependency-aware startup
- Production-safe workload scaling

---

# Repository structure

```

.
├── Jenkinsfile
├── job-config.xml
└── README.md

```

---

# Use cases

Typical scenarios include:

- Planned maintenance
- Infrastructure upgrades
- Cluster migration
- Disaster recovery testing
- Controlled application shutdown
- Environment suspension during non-working hours

---

# Requirements

- Jenkins Pipeline
- OpenShift CLI (`oc`)
- Jenkins Credentials
- Access to OpenShift API
- Sufficient permissions to scale Deployments

---

# Summary

This project demonstrates how a production OpenShift environment can be safely suspended and restored using a single Jenkins Pipeline.

Instead of treating workload scaling as a simple automation task, the pipeline focuses on operational safety, deployment dependency ordering, state preservation, and predictable recovery — principles that become increasingly important as application environments grow in size and complexity.