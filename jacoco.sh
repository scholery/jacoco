read -p "请输入trace-id：" trace
read -p "是否reset(Y/N)：" reset
read -p "java版本(默认8,可选:11,1,7,21)：" jversion

#traceId="default_trace_id"
file="default"
traceIdParam=""
if [ -n "$trace" ] 
then
 traceIdParam="--traceId $trace"
 file="$trace"
fi
echo "trace is: $traceIdParam"

r=""
if [ "$reset" = "Y" ]
then
 r="--reset"
fi
java="java8"
port="6308"
if [ -n "$jversion" ]
then
java="java$jversion"
    if [ "$jversion" != "8" ]
    then
    port="63$jversion"
    fi
fi
#echo $r
#echo $java
#echo $port
jarpath=/Users/u0046326/workspace/bba/github/jacoco/org.jacoco.cli/target/org.jacoco.cli-0.8.12-SNAPSHOT-nodeps.jar
source=/Users/u0046326/workspace/bba/demo/$java/

java -jar $jarpath dump --destfile /Users/u0046326/workspace/bba/test_data_jacoco/$java/$file.exec $traceIdParam $r --port $port
#echo $execFile

java -jar $jarpath report /Users/u0046326/workspace/bba/test_data_jacoco/$java/$file.exec --classfiles $source/target/classes --sourcefiles $source/src/main/java --html /Users/u0046326/workspace/bba/test_data_jacoco/$java/$file $traceIdParam