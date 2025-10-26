# Writing tests

1. Each unit test should be self-contained and independent of other tests. Tests should be able to run in parallel. It's fine to use shared fixture functions.
2. Use org.junit.jupiter.api.Assertions for assertions.
3. Use io.heapy.komok.tech.logging.Logger for logging.
4. Refuse to unit test code that requires too many mocks because of bad structure and request to update code.
5. Assert Whole Objects, Not Individual Fields.
