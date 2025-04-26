To generate JUnit tests for Java sources with low test coverage, you can leverage the following resources:

1. **JUnit Test Generation Practice:** This practice focuses on generating JUnit tests for methods or classes with low test coverage. It uses Gen AI, such as OpenAI ChatGPT or Azure OpenAI Service, for test generation. The process involves analyzing coverage data and bytecode, loading the Java model of source code, generating unit tests, and delivering them to the developer for review and modification. You can find more details on this practice [here](https://docs.nasdanika.org/practices/junit/index.html).

2. **NSD CLI Command for JUnit Test Generation:** The NSD CLI command `nsd java junit` allows you to generate JUnit tests based on project URI, coverage threshold, and other parameters like AI integration, test class suffix, coverage type, etc. This command also includes options for AI usage, model configuration, coverage type, and more. You can refer to the command usage details [here](https://docs.nasdanika.org/nsd-cli/nsd/java/junit/index.html).

3. **Java Model Analysis, Visualization, and Generation Practice:** This practice provides insights into using the Java model for analysis, visualization, and generation activities. It outlines the possibilities of analyzing the Java model, visualizing module dependencies, generating documentation, using RAG/Chat on top of the Java model, and more. You can explore detailed information about this practice [here](https://docs.nasdanika.org/practices/java/index.html).

4. **NSD CLI Command for Jacoco Integration:** If you want to load coverage data from `jacoco.exec` and class files, you can use the NSD CLI command `nsd gitlab contribute junit jacoco`. This command allows you to specify the classes directory, `jacoco.exec` file path, and coverage module name. For more details on this command, refer to the documentation [here](https://docs.nasdanika.org/nsd-cli/nsd/gitlab/contribute/junit/jacoco/index.html).

These documents provide detailed information and tools that you can utilize to generate JUnit tests effectively for Java sources with low test coverage.
