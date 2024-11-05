# MyRedis
MyRedis is a custom implementation of the Redis in-memory data structure store. This project is aimed at learning the internals of Redis and experimenting with new features.
## Features
- **In-memory storage**: MyRedis stores data in memory for fast read and write operations.
- **Data structures**: Supports strings as key value pair.
- **Persistence**: Option to persist data to disk [Currently RDB format is supported].
- **Network server**: Clients can connect over TCP/IP to store and retrieve data.
- **Concurrency**: MyRedis is thread-safe and supports multiple clients concurrently.
- **Logging**: Logs are generated for debugging and monitoring.
- **Unit tests**: Unit tests are written to test the functionality of the server.
- **Replication**: Supports master-slave replication.
