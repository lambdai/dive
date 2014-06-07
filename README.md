dive
===========

dive is my hive-like project. It accept DML and DDL base on SQL. It runs data processing on hadoop.




* current working
 
See issues https://github.com/silentdai/dive/issues

* Milestone 0.1

 * SQL input: support natural join, predicate, group by and projection.

 * Data processing: run N + X map/reduce jobs on each SELECT. N is the 2-way join, X is the aggregation or projection.

* History

This idea origins from the final project of  database course. I need practise of implementing SQL and map-reduce. 

The original repository is github.com/silentdai/mapred_jobs. As dive grows to more than 4,000 lines, it should keep indpendent of other experimental code.
