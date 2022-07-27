# Soteria - Privacy-Preserving Machine Learning for Apache Spark

Soteria is a privacy-preserving machine learning solution developed on top of [Apache Spark](https://github.com/apache/spark) and resorting to [Graphene-SGX](https://github.com/oscarlab/graphene) (currently named [Gramine](https://github.com/gramineproject/gramine)).

Build upon the concept of computation partitioning, Soteria allows running machine learning workloads inside the enclaves while running non-sensitive code outside. 

Furthermore, the main goal of Soteria, besides providing alternatives for state-of-the-art solutions is to improve the security of running these workloads in the real world. 

**Warning**: This is an academic proof-of-concept prototype and has not received careful code review. This implementation is NOT ready for production use.


## Machine Learning and Attacks




## Architecture

The architecture of Soteria consists of two main designs, SML-1 and SML-2. 

![Soteria architecture and operations flow.]((https://github.com/claudiavmbrito/Soteria/tree/main/images/arch_soteria.pdf))

SML-1 intends to run all the workloads inside the enclaves, with both master and worker nodes running inside the enclaves.

SML-2 resorts to the partitioning of computation between what runs inside the enclaves and outside the enclaves. With this, a single worker node becomes a double worker node, i.e., two workers run on the node, with one running inside the enclaves and the other outside the enclave. This mechanism reduces the amount of trusted code base to be run inside the enclaves which intends to reduce the overhead imposed by large amounts of code running inside SGX.


## Getting Started

The code for "Soteria: Privacy-Preserving Machine Learning for Apache Spark" will be published here soon.


## Security Proofs

In [`proofs`](https://github.com/claudiavmbrito/Soteria/tree/main/proofs), we present the security proofs of Soteria. Here, we discuss the security protocol followed by Soteria and define it formally. 

It is divided into two main sections: Section A present the full proof of Soteria for all components and Section B depicts the ML attacks and in which circumstances Soteria is secure against each attack. 
