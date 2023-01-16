# Community Detection
This is an implementation of two state-of-the-art stream-based community detection algorithms for large-scale networks.
The streaming approach allows for the graph's edges to be processed as a stream, thus avoiding to store the entire graph into memory, which would not be practical.

## SCoDA
**SCoDA [1]** is a linear time, linear space, stream-based algorithm for community detection which requires a random permutation of the edges before processing the stream. It is based on the idea that a randomly choosen edge *e* is more likely to connect edges of the same community.

## CoEuS
**CoEuS [2]** is an efficient, stream-based algorithm for community detection which is based on seed-set expansion. Communities are expandend based on the nodes in their respective seed-set.

## Usage
The helper messages shows the available datasets that can be processed. It is also possible to provie a different (SNAP) dataset if it is in the right format and in the appropriate directory.
*See helper message*


## Datasets
The datasets used for the analyses are networks with ground-truth communities from [Stanford Large Network Dataset Collection (SNAP)](https://snap.stanford.edu/data/index.html) **[3]**


# Bigliography
[1] *Alexandre Hollocou, Julien Maudet, Thomas Bonald, and Marc Lelarge. 2017. A linear streaming algorithm for community detection in very large networks. CoRR (2017).*
[2] *Panagiotis Liakos, Alexandros Ntoulas and Alex Delis. 2017. COEUS:community detection via seed-set expansion on graph streams. In 2017 IEEE International Conference on Big Data (Big Data).*
*[3] Jure Leskovec and Andrej Krevl. 2014. Snap Datasets: Standford Large Network Dataset Collection.*
