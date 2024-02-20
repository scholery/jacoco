read -p "请输入trace-id：" trace
read -p "是否reset(Y/N)：" reset

traceId="default_trace_id"
if [ -n "$trace" ] 
then
 traceId="$trace"
fi
echo "trace-id is: $traceId"

r=""
if [ $reset = "Y" ] 
then
 r="--reset"
fi
echo $r

jarpath=/Users/u0046326/workspace/bba/github/jacoco/org.jacoco.cli/target/org.jacoco.cli-0.8.12-SNAPSHOT-nodeps.jar

java -jar $jarpath dump --destfile /Users/u0046326/workspace/bba/test_data_jacoco/$traceId.exec --traceId $traceId $r
#echo $execFile

java -jar $jarpath report /Users/u0046326/workspace/bba/test_data_jacoco/$traceId.exec --classfiles /Users/u0046326/workspace/bba/demo/target/classes --sourcefiles /Users/u0046326/workspace/bba/demo/src/main/java --html /Users/u0046326/workspace/bba/test_data_jacoco/$traceId  --traceId $traceId