STDBSCAN
========

This is a modification of RapidMiners native DBSCAN algorithm. IT allows you to cluster points according to ther geografical as well all temporal distance. 

INPUNT
======

The algorithm takes epsilon_time and epsilon_space to built the clusters. It is also possible to define the dimension of those values.

The temporal dimension should be passed as timestamps measured from a certain point in time in milliseconds.

The distance in space is calculated from GPS (long- and latitude attribute) making use of the Haversine formula (http://en.wikipedia.org/wiki/Haversine_formula)
