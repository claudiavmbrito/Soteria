# SOTERIA: Privacy-Preserving Machine Learning for Apache Spark

**SOTERIA** is a privacy-preserving machine learning solution developed on top of [Apache Spark](https://github.com/apache/spark) using Intel SGX for secure computation. It employs computation partitioning to perform sensitive computing tasks within secure enclaves and non-sensitive tasks outside.
The main goal of SOTERIA, besides providing alternatives for state-of-the-art solutions is to improve the security of running these workloads in the real world.

**Warning 1**: This repository is being updated.

**Warning 2**: This is an academic proof-of-concept prototype and has not received careful code review. This implementation is NOT ready for production use.

**Warning 3**: Please be aware that this proof-of-concept is based on Graphene's implementation v1.0. Newer versions may be incompatible. 

**Version Note**: This is SOTERIA v1.0, implementing the core SGX-based privacy-preserving ML framework with computation partitioning. Advanced security features like attack detection and pattern analysis are planned for future versions.

### Installation

Follow these steps to set up SOTERIA:

1. **Install SGX Drivers and SDK**: Run the `install_sgx.sh` script.
   ```bash
   sudo bash scripts/install_sgx.sh
   ```

2. **Set up Cloudera Cluster**: SOTERIA was tested with Cloudera. Run the `install_cluster.sh` script.
   ```bash
   sudo bash scripts/install_cluster.sh
   ```

3. **Build Spark with Graphene**: Navigate to the `graphene-sgx-spark/spark` directory and use `make`.
   ```bash
   cd graphene-sgx-spark/spark
   make
   ```

### Usage

SOTERIA supports several machine learning workflows. Here's how you can use it:

1. **Create a SOTERIA Session**:
   ```scala
   import soteria.core.SoteriaCore
   val session = SoteriaCore.createSession("SoteriaApp")
   ```

2. **Load Data**:
   ```scala
   val encryptedDataset = session.loadEncryptedDataset[TrainingData]("path/to/data")
   ```

3. **Train a Model**:
   ```scala
   import soteria.ml.SoteriaML._
   val logisticRegression = new SoteriaLogisticRegression(session)
   val model = logisticRegression.train(encryptedDataset)
   ```

4. **Perform Inference**:
   ```scala
   val prediction = model.predict(features)
   ```

5. **Close Session**:
   ```scala
   session.close()
   ```

### Full paper

For more information, please see:
IEEE Xplore: [Privacy-Preserving Machine Learning on Apache Spark](https://ieeexplore.ieee.org/document/10314994)

If you need to cite our work:
```
@inproceedings{brito2023soteria,
  title={SOTERIA: Preserving Privacy in Distributed Machine Learning},
  author={Brito, Cl{\'a}udia and Ferreira, Pedro and Portela, Bernardo and Oliveira, Rui and Paulo, Jo{\~a}o},
  booktitle={Proceedings of the 38th ACM/SIGAPP Symposium on Applied Computing},
  pages={135--142},
  year={2023}
}
```


--------

## Dependencies

SOTERIA is implemented in Scala, Java, and C. It requires:
- Intel SGX SDK
- Apache Spark
- Gramine (formerly Graphene)

Tested OS: Ubuntu 18.04 SP2

**NOTE**: This has been tested in Ubuntu 18.04, it has not been tested in newer OS versions.

### Apache Spark

To install Apache Spark to test the vanilla version, please run and see `build.sh` in [`scripts`](https://github.com/claudiavmbrito/Soteria/tree/main/scripts).

#### Data Encryption

For easy to use encryption, we implement an encryption mechanism based on AES-GCM 128. Such file is implemented inside of Apache Spark allowing its broad use outside SOTERIA.


### Intel SGX

To install SGX SDK and its Driver, please see `install_sgx.sh` and run:

```
bash ./install_sgx.sh
```

### Gramine 

- To use the previous and base code of Gramine used to develop SOTERIA, please refer to https://github.com/gramineproject/gramine/tree/v1.0.
- To use the updated version of Gramine, follow [Gramine](https://github.com/gramineproject/gramine) documentation. 
- The manifest files need to be carefully changed to work with the new versions of Gramine. 
---

### Cluster in Cloudera 

To install Cloudera version for which SOTERIA was tested, please see `install_cluster.sh` and run:

```
bash ./install_cluster.sh
```

Then, change the Manifest directories accordingly.

<!--
___
## Overview

### Machine Learning and Attacks
SOTERIA was built based on the current attacks to the machine learning pipeline as seen in the figure below. 
Specifically, we will consider Adversarial Attacks, Model Extraction, Model Inversion and Membership Inference, and Reconstruction Attacks. 

<p align="center">
    <img src="images/ml_pipeline_refactor-1.png" alt="SOTERIA Architecture" title="Machine Learning Pipeline and Attacks">
</p>

### Architecture

As depicted in Figure 2 by the gray boxes, a Spark cluster is composed of a Master and several Worker nodes.
The architecture of SOTERIA consists of two main designs, SOTERIA-B (baseline) and SOTERIA-P (computation partitioning). 

<p align="center">
    <img src="images/arch_soteria_poster-1.png" alt="SOTERIA Architecture" title="SOTERIA Architecture and Flow">
</p>

SOTERIA-B intends to run all the workloads inside the enclaves, with both master and worker nodes running inside the enclaves.

SOTERIA-P resorts to the partitioning of computation between what runs inside the enclaves and outside the enclaves. With this, a single worker node becomes a double worker node, i.e., two workers run on the node, with one running inside the enclaves and the other outside the enclave. This mechanism reduces the amount of trusted code base to be run inside the enclaves which intends to reduce the overhead imposed by large amounts of code running inside SGX.

<p align="center">
    <img src="images/spark-sml2-1.png" alt="SOTERIA Designs" title="SOTERIA Twofold Worker Design">
</p>

### Security Proofs

In [`proofs`](https://github.com/claudiavmbrito/Soteria/tree/main/proofs), you can find the security proofs of SOTERIA. We discuss the security protocol followed by SOTERIA and define it formally. 

It is divided into two main sections: Section A present the full proof of SOTERIA for all components and Section B depicts the ML attacks and in which circumstances SOTERIA is secure against each attack. 
___

## Getting Started

### Dependencies

SOTERIA is mainly written in Scala, JAVA and C and was built and tested with Intel's SGX SDK `2.6`, SGX Driver `1.8` and Gramine `1.0` (previously named Graphene-SGX).

### Apache Spark

To install Apache Spark to test the vanilla version, please run and see `build.sh` in [`scripts`](https://github.com/claudiavmbrito/Soteria/tree/main/scripts).

#### Data Encryption

For easy to use encryption, we implement an encryption mechanism based on AES-GCM 128. Such file is implemented inside of Apache Spark allowing its broad use outside SOTERIA.


### Intel SGX

To install SGX SDK and its Driver, please see `install_sgx.sh` and run:

```
bash ./install_sgx.sh
```

### Gramine 

- To use the previous and base code of Gramine used to develop SOTERIA, please refer to https://github.com/gramineproject/gramine/tree/v1.0.
- To use the updated version of Gramine, follow [Gramine](https://github.com/gramineproject/gramine) documentation. 
- The manifest files need to be carefully changed to work with the new versions of Gramine. 
---

### Cluster in Cloudera 

To install Cloudera version for which SOTERIA was tested, please see `install_cluster.sh` and run:

```
bash ./install_cluster.sh
```

Then, change the Manifest directories accordingly.

___
-->

## Contact

Please contact us at `claudia.v.brito@inesctec.pt` with any questions.
