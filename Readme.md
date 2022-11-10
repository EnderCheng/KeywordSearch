## Read Me
This is the source code for the paper "Multi-client Secure DPF-based Keyword Search with Practical Performance Trade-offs" submitted to TDSC.

### How to Use
The source code is a Java project with Gradle built using IntelliJ IDEA 2022.2.3 (Community Edition).

Please directly import the project into IntelliJ IDEA 2022.2.3.

### Code Description

The source code implements the baseline scheme, the first construction, and the variant construction presented in the paper.

To test them, you first need to set parameters as follows:

```java
        String path = "/Users/cheng/IdeaProjects/KeyPhraseSearch/Datasets/sample_keywords.csv"; // path to the plaintext keyword index
        int max_size = 1000; //maximum supported keywords for one document
        int doc_size = 4000; //number of documents
        int mode = 2; // False Positive Rate - {1: 10^-3}, {2: 10^-4}, {3: 10^-5}, {4: 10^-6}")
        String[] search_keywords = new String[]{"english"}; // query keywords
```

To test the baseline scheme, you can run

```java
        Baseline bl_test = new Baseline(path, max_size, doc_size, mode, search_keywords);
        boolean isMAC = true; // using MAC to provide verifiability
        bl_test.BuildIndex(isMAC); // building an outsourced keyword index
        bl_test.CreateMAC(isMAC); // building MACs for an outsourced keyword index
        bl_test.SearchTest(isMAC); // searching keywords
        bl_test.UpdateSim(isMAC); // updating one document
```
To test the first construction, you can run

```java
        int q = 5; // CFF parameter - number of shares
        int d = 2; // CFF parameter - collusion resistance
        ODPF odpf = new ODPF(path, max_size, doc_size, mode, search_keywords, q, d);
        boolean isMAC = true; // using MAC to provide verifiability
        odpf.BuildIndex(isMAC); // building an outsourced keyword index
        odpf.CreateMAC(isMAC); // building MACs for an outsourced keyword index
        odpf.SearchTest(isMAC, M); // searching keywords
        odpf.UpdateSim(isMAC); // updating one document
```

To test the variant construction, you can run
```java
        int q = 5; // CFF parameter - number of shares
        int d = 2; // CFF parameter - collusion resistance
        int M = 10; // number of segmentation
        double loadFactor = 0.9 // loadfactor of Cuckoo Filter
        VDPF vdpf = new VDPF(path, max_size, doc_size, mode, search_keywords, q, d, M, loadFactor);
        boolean isMAC = true; // using MAC to provide verifiability
        vdpf.BuildIndex(isMAC); // building an outsourced keyword index
        vdpf.CreateMAC(isMAC); // building MACs for an outsourced keyword index
        vdpf.SearchTest(isMAC); // searching keywords
        vdpf.UpdateSim(isMAC); // updating one document
```

#### Output Example:
```
Generating Cover Family Matrix...
Number of MACs per column: 25
Length of GBF-encoded Keywords: 429440
Starting Encoding and Encryption...
Generating keys for encrypting columns...
100% ################################################## |
Start writing VDPF_col_keys_1000_4000_10_5_2.csv...
100% ################################################## |
Generating keys for encrypting rows...
100% ################################################## |
Start writing VDPF_row_keys_original_1000_4000_10_5_2.csv...
100% ################################################## |
Starting converting...
100% ################################################## |
```

### Note

Since no database is integrated into the source code, all processed datasets are loaded into memory. 
If you set $`N \times n > 5000000`$, it may cause heap overflow. 
