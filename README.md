# Soteria: Privacy-Preserving Machine Learning for Apache Spark

**Soteria** is a privacy-preserving machine learning solution developed on top of [Apache Spark](https://github.com/apache/spark) and resorting to [Graphene-SGX](https://github.com/oscarlab/graphene) (currently named [Gramine](https://github.com/gramineproject/gramine)).

Build upon the concept of computation partitioning, Soteria allows running machine learning workloads inside the enclaves while running non-sensitive code outside. 

Furthermore, the main goal of Soteria, besides providing alternatives for state-of-the-art solutions is to improve the security of running these workloads in the real world. 

**Warning**: This is an academic proof-of-concept prototype and has not received careful code review. This implementation is NOT ready for production use.

___
## Machine Learning and Attacks
Soteria was built based on the current attacks to the machine learning pipeline as seen in the figure below. 
Specifically, we will consider Adversarial Attacks, Model Extraction, Model Inversion and Membership Inference, and Reconstruction Attacks. 

<p align="center">
    <img src="images/ml_pipeline_refactor-1.png" alt="Soteria Architecture" title="Machine Learning Pipeline and Attacks">
</p>



## Architecture

As depicted in Figure \ref{fig_architecture} by the gray boxes, a Spark cluster is composed of a Master and several Worker nodes.
The architecture of Soteria consists of two main designs, SML-1 and SML-2. 

<p align="center">
    <img src="images/arch_soteria_poster-1.png" alt="Soteria Architecture" title="Soteria Architecture and Flow">
</p>

SML-1 intends to run all the workloads inside the enclaves, with both master and worker nodes running inside the enclaves.

SML-2 resorts to the partitioning of computation between what runs inside the enclaves and outside the enclaves. With this, a single worker node becomes a double worker node, i.e., two workers run on the node, with one running inside the enclaves and the other outside the enclave. This mechanism reduces the amount of trusted code base to be run inside the enclaves which intends to reduce the overhead imposed by large amounts of code running inside SGX.

<p align="center">
    <img src="images/spark-sml2-1.png" alt="Soteria Designs" title="Soteria Twofold Worker Design">
</p>

## Security Proofs

In [`proofs`](https://github.com/claudiavmbrito/Soteria/tree/main/proofs), you can find the security proofs of Soteria. We discuss the security protocol followed by Soteria and define it formally. 

It is divided into two main sections: Section A present the full proof of Soteria for all components and Section B depicts the ML attacks and in which circumstances Soteria is secure against each attack. 

___

## Getting Started

#### Apache Spark

To install Apache Spark to test the native version, please run and see `build.sh` in [`scripts`](https://github.com/claudiavmbrito/Soteria/tree/main/scripts)

The code for "Soteria: Privacy-Preserving Machine Learning for Apache Spark" will be published here soon.

#### Dependencies

Soteria is mainly written in Scala, JAVA and C and was built and tested with Intel's SGX SDK `2.8.100.3`, SGX Driver `2.6.0` and Gramine `1.0` (previously named Graphene-SGX).

___

## Contact

Please contact us at `claudia.v.brito@inesctec.pt` with any questions.