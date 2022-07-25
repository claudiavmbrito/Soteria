# Soteria - Privacy-Preserving Machine Learning for Apache Spark

Soteria is a privacy-preserving machine learning solution developed on top of [Apache Spark](https://github.com/apache/spark) and resorting to [Graphene-SGX](https://github.com/oscarlab/graphene) (currently named [Gramine](https://github.com/gramineproject/gramine)).

Build upon the concept of computation partitioning, Soteria allows running machine learning workloads inside the enclaves while running non-sensitive code outside. 

Furthermore, the main goal of Soteria, besides providing alternatives for state-of-the-art solutions is to improve the security of running these workloads in the real world. 

**Warning**: This is an academic proof-of-concept prototype and has not received careful code review. This implementation is NOT ready for production use.


## Machine Learning and Attacks



## Architecture

The architecture of Soteria consists of two main designs, SML-1 and SML-2. 
SML-1 intends to run all the workloads inside the enclaves 


## Getting Started

The code for "Soteria: Privacy-Preserving Machine Learning for Apache Spark" will be published here soon.




## Security Proofs

In [`proofs`](https://github.com/claudiavmbrito/Soteria/proofs), we present the security proofs of Soteria. Here, we discuss the security protocol followed by Soteria and define it formally. 

It is divided into two main sections: Section A present the full proof of Soteria for all components and Section B depicts the ML attacks and in which circumstances Soteria is secure against each attack. 
