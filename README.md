# Repository for custom workflows #

### What is this repository for? ###

This is a java project for custom workflows. It has a sample implementation of a custom workflow in a sub-module 'custom-workflow'.
You can create you own sub-modules with other implementations and remove the sample.

### How does it work? ###

* You implement a custom workflow as a new sub-module in this project following the documentation on [Overview of Workflow Customizations](https://docs.reltio.com/en/engage/manage-data-workflows/overview-of-workflow-customizations);
* Push changes into a new branch and create a pull request to the master (JARs are built on merges to the master branch only);
  * Maintain proper versioning of your custom workflow in the pom.xml file (`<version>1.0-SNAPSHOT</version>`) to test changes on dev/test tenants before 
  registering the updated JAR on production tenant.
* The pull request is analyzed by automatic pipeline and if is successful marks the pull request accordingly. Default reviewers
may review and approve the pull request after that. The pipeline verifies your changes on different type of issues: 
code style, unsafe libraries, coverage of the code by unit tests, etc;
* Once the pull request is approved by anyone from default reviewers and passed the pipeline it becomes possible to merge it;
* After the merge users from email.list property in repository.properties are notified that the jar is built and ready for deploy on a tenant.

### Who do I talk to? ###

There is [Workflow FAQ](https://docs.reltio.com/en/engage/manage-data-workflows/overview-of-workflow-customizations/workflow-faq) page that can address some questions.

In case of any problems and questions you can leave a comment in your pull request and default reviewers will be notified.

You might need access to [Sonar Cloud](https://sonarcloud.io/) to see detailed reports for issues found by the pipeline, please let us know about it in a pull request.