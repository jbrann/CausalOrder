#!/bin/bash -ex

IMAGE=${IMAGE:-CausalOrder:master}
docker run --rm -v $PWD/../../..:/tmp/ses $IMAGE \
	/tmp/ses/src/test/java/org/brann/clock/test/test.txt | \
	grep -v 'Time to complete tests' > \
	output.txt
diff -u output.txt reference-output.txt
