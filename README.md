# fxengine
order book and matching engine for fx trades

Currently processing 1.2 million orders per second on AWS m6a.2xlarge EC2 instance (4 core, 8vCPU and 32Gb RAM)

taskset -c 0-7 mvn exec:exec@test

^above command executes the test class utilising all four cores including their virtual cores.


# TODO

- ScyllaDB for persistence
- Logging
- Monitoring
- Additional fine tuning
- Javadoc for the rest of the classes
