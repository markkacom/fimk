#!/bin/sh
java -cp lib/h2*.jar org.h2.tools.Shell -url jdbc:h2:fim_test_db/fim -user sa -password sa
