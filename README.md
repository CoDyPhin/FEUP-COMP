# GROUP: G5F


### Evaluation

| Name           | Number    | Grade | Contribution |
| -------------- | --------- | ----- | ------------ |
| Abel Tiago     | ei11102   | 20    | 25%          |
| Carlos Lousada | 201806302 | 20    | 25%          |
| José Rocha     | 201806371 | 20    | 25%          |
| Tomás Mendes   | 201806522 | 20    | 25%          |

**Global Grade of The Project:** 19

 

### Summary

The aim of this project is to parse and run a .jmm file. This is achieved by building an abstract syntax tree (AST), which is then analyzed in order to find possible syntactical/semantical errors and converted into OLLIR code. This code is then converted into an OLLIR Class, which then is converted into JVM Code and compiled into a .class file, which can be executed.



### Dealing with syntactic errors

 Upon encountering a syntactical error, the parser terminates the program and adds the error to the reports list (as a syntactical error).



### Semantic Analysis

The semantic analysis verifies everything that was required in checklist for CP2:

- **Expression Analysis**
  - Verifies if operations are of the same type (e.g. int + boolean gives an error);
  - Arithmetic operations can't use direct array access (e.g. array1 + array2);
  - Verifies if an array access is done in an array (e.g. 1[10] isn't allowed);
  - Verifies if an array index is, in fact, an integer (e.g. a[true] isn't allowed); 
  - Verifies if the assignee's value is equal to assigned's value (a_int = b_boolean isn't allowed);
  - Verifies if boolean operations (&&, < or !) only contain booleans;
  - Verifies if conditional expressions (if and while) result in a boolean;




- **Method Verification**
  - Verifies if a target's method exists, and it it contains a method (e.g. a.foo, checks it 'a' exists and if there is a method 'foo') 
    
    - In case it is the declared class's type (e.g. using this), if there isn't a declaration in the code and if it doesn't extend another class it returns an error. If it does extend another class then we just assume the method is from the super class.
    
  - In case the method isn't from the declared class, that is, is from the imported class, we assume it exists and assume the correct types (e.g. a = Foo.b(), if a is an integer, and Foo is an imported class, we assume the b method is static, that is doesn't have arguments and that it returns an integer).
  
- If there is no way to know the type of the method (e.g. Foo.b(Foo.a()), if Foo is an imported class, we can't know what Foo.a() returns) we just assume if returns void in OLLIR (.V) which will fail in most cases later.
  
  - Verifies if the number of arguments in the invocation is the same as the parameters in the declaration;
  
  - Verifies if the type of the parameters is the same as the type of the arguments;
  
    
  
- **Extras**
  
  - This parser is capable of dealing with method overloading.
  - We created extra tests:
    - 6 tests showcasing how powerful our compiler is (./test/fixtures/public/checkpoint3)
    - 2 tests showcasing instruction selection in Jasmin (./test/fixtures/public/IfCondOptimization.jmm and ./test/fixtures/public/IIncOptimization.jmm)



### Code Generation

 The .jmm code starts by being parsed into an abstract syntax tree (AST), which is then used to generate OLLIR code. This code is later converted into an OLLIR class, which allows the Jasmin code (JVM) to be generated and executed. There might be some issues if something goes wrong during a certain stage of the code (e.g. if the semantical analysis fails to identify a semantical error or if the OLLIR Code has syntatical errors).



### Task Distribution

| Name           | Tasks                                                        |
| -------------- | ------------------------------------------------------------ |
| Abel Tiago     | AST (CP1), OLLIR (CP2), Jasmin (CP2), Jasmin (CP3), Jasmin Optimizations (Final Delivery) |
| Carlos Lousada | AST (CP1), Jasmin (CP2), Jasmin (CP3), Jasmin Optimizations (Final Delivery), OLLIR Optimizations (Final Delivery) |
| José Rocha     | AST (CP1), Semantic Analysis (CP2), OLLIR (CP2), OLLIR (CP3), OLLIR Optimizations (Final Delivery) |
| Tomás Mendes   | AST (CP1), Semantic Analysis (CP2), OLLIR (CP3), OLLIR Optimizations (Final Delivery) |



### Pros

- The compiler contains some of the suggested OLLIR optimizations: -o (Loop templates) and -r (Register Allocation w/o Life time analysis and Graph Coloring Algorithm);
- The compiler contains some of the suggested Jasmin optimizations: Instruction Selection (distinguish between *iconst*, *bipush*, *sipush*, *ldc*; distinguish between *iinc* and *iadd*; distinguish between *if_icmp<cond>* and *if<cond>*);
- The compiler also contains some extra optimizations, concerning both OLLIR and Jasmin:
  - OLLIR
    - Aux variable recycling and variable sanitization;
  - Jasmin
    - Not (Replaces condition branches and goto instructions with *isub*);
    - *iinc* may be used not only for jmm instructions such as *i = i + 1* but also *i = i + n*, *i = n + i* and *i = i - n*, *n* being a constant of one byte between -32768 e 32767;
    - Even though jmm is only capable of dealing with less than instructions, our compiler may use *ifle*, *iflt*, *ifge*, *ifgt*, *ifeq* for instructions such as *0 >= i*, *i < 0*, *i >= 0*, *0 < i*, *i == 0*, respectively.
- OLLIR and Jasmin are well implemented and should be able to run code with complex expressions.



### Cons

- The compiler could have used more optimizations, such as constant propagation;
- The Ollir Code can sometimes fail to parse certain expressions - it can either parse extremely complex expressions but fail to parse simple expressions due to small mistakes when converting the AST to OLLIR.

