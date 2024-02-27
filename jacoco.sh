read -p "请输入trace-id：" trace
read -p "是否reset(Y/N)：" reset

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
#echo $r

jarpath=/Users/u0046326/workspace/bba/github/jacoco/org.jacoco.cli/target/org.jacoco.cli-0.8.12-SNAPSHOT-nodeps.jar

java -jar $jarpath dump --destfile /Users/u0046326/workspace/bba/test_data_jacoco/$file.exec $traceIdParam $r
#echo $execFile

java -jar $jarpath report /Users/u0046326/workspace/bba/test_data_jacoco/$file.exec --classfiles /Users/u0046326/workspace/bba/demo/target/classes --sourcefiles /Users/u0046326/workspace/bba/demo/src/main/java --html /Users/u0046326/workspace/bba/test_data_jacoco/$file $traceIdParam