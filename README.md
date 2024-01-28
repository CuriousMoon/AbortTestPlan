# AbortTestPlan

AbortTestPlan project creates a test plan consisting of failed and not executed test cases(in case jenkins build is aborted in between execution). 
We can use this test plan to rebuild our Jenkins job.

More detailed explaination of the project
> https://medium.com/@guptamansi101/ed60d749571

## Steps To Start

### 1. Cloning:-
```sh
$ git clone -b master https://github.com/CuriousMoon/AbortTestPlan.git 
```

### 2. Execution Maven Command:-
```sh
$ mvn clean test -DplanJiraId=$planJiraId -DprojectName=$projectName -DfilePath=$filePath
```

Now, let us understand the parameters passed in above command.

1. **planJiraId** : This is the original test plan id from which we will be creating retry test plan. This is the test id from which we have originally executed Jenkins job


2. **projectName** : This is the project name to which we have to attach retry test plan with.


3. **filePath** : This file path will have a file which will contain the list of the cases which have their status marked as pass. We can generate this file using ITestListener to log the pass test case name in the file. See the shared blog link for more details.
