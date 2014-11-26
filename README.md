STDBSCAN                                       
========
This is a extension of RapidMiners native DBSCAN algorithm. 

Input
======
While in classic DBSCAN epsilon is used to determine the maximal distance between two density-related points this modification introduces ** epsilon_space ** and ** epsilon_time **. This allows you to cluster data according to their geografical as well all temporal distance.

Furthermore it is possible to choose from several dimension for this imput parameters like meters, kilometers and so on for spatial and seconds, minutes, hours and more for temporal distance.
