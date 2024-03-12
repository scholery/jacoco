#4win.sh需要注意的是，路径和linux一样，均为正斜杠，而非windows的反斜杠
read -p "是否reset(Y/N)：" reset
read -p "请输入batchId：" batchId

r=""
if [ $reset = "Y" ] 
then
 r="--reset"
fi
#echo $r

#业务处理：根据batchId获得caseId
caseIds=()
if [ $batchId = "batchId001" ]
then
 caseIds=(
 "batchId001_caseId001"
 "batchId001_caseId002"
 )
fi
if [ $batchId = "batchId002" ]
then
 caseIds=(
 "batchId002_caseId003"
 "batchId002_caseId004"
 )
fi
if [ $batchId = "batchId003" ]
then
 caseIds=(
 "batchId003_caseId005"
 "batchId003_caseId006"
 )
fi
if [ $batchId = "batchId004" ]
then
 caseIds=(
 "batchId004_caseId002"
 "batchId004_caseId003"
 )
fi
echo  "*****$batchId中caseIds个数为: ${#caseIds[*]}"
echo  "*****$batchId中caseIds元素为: ${caseIds[*]}"


#入参是batchId，根据batchId得到caseId（本地脚本先配置），将所有caseId的合并成一个batchId的文件，然后导出该batchId的report
clientJarPath=C:/workBackup/idea/IdeaProjects/JingZhunCeShi/jacoco/org.jacoco.cli/target/org.jacoco.cli-0.8.12-SNAPSHOT-nodeps.jar
jacocoExecFilePath=C:/workBackup/idea/IdeaProjects/JingZhunCeShi/jacoco/jacocoReport/execFile
jacocoHtmlReportPath=C:/workBackup/idea/IdeaProjects/JingZhunCeShi/jacoco/jacocoReport/htmlReport
##xmlReport路径必须已存在，否则生成报告时候会报错
jacocoTestCommonPath=C:/workBackup/idea/IdeaProjects/JingZhunCeShi/jacoco-test/common
jacocoTestJava8Path=C:/workBackup/idea/IdeaProjects/JingZhunCeShi/jacoco-test/java8

#遍历caseIds，每个caseId生成一个exec文件
for caseId in ${caseIds[@]}
do
  java -jar $clientJarPath dump --destfile $jacocoExecFilePath/$caseId.exec --traceId $caseId $r
  #单独生成每个caseId的报告
  java -jar $clientJarPath report $jacocoExecFilePath/$caseId.exec --classfiles $jacocoTestJava8Path/target/classes --classfiles $jacocoTestCommonPath/target/classes --sourcefiles $jacocoTestJava8Path/src/main/java --sourcefiles $jacocoTestCommonPath/src/main/java --html $jacocoHtmlReportPath/$caseId --traceId $caseId
  echo "*****CurrentExecFile:$jacocoHtmlReportPath/execFile/$caseId.exec "
  allExecFiles=$(echo $allExecFiles $jacocoExecFilePath/$caseId.exec )
done
echo "*****AllExecFiles:$allExecFiles"

#exec文件数量目前测试到300是支持的
#通过多个exec文件合成html报告
java -jar $clientJarPath report $allExecFiles  --classfiles $jacocoTestJava8Path/target/classes --classfiles $jacocoTestCommonPath/target/classes --sourcefiles $jacocoTestJava8Path/src/main/java --sourcefiles $jacocoTestCommonPath/src/main/java --html $jacocoHtmlReportPath/$batchId ##--caseId $caseId
#通过多个exec文件合成xml报告
java -jar $clientJarPath report $allExecFiles  --classfiles $jacocoTestJava8Path/target/classes --classfiles $jacocoTestCommonPath/target/classes --sourcefiles $jacocoTestJava8Path/src/main/java --sourcefiles $jacocoTestCommonPath/src/main/java --xml $jacocoHtmlReportPath/$batchId.xml ##--caseId $caseId
#通过多个exec文件合成csv报告
java -jar $clientJarPath report $allExecFiles  --classfiles $jacocoTestJava8Path/target/classes --classfiles $jacocoTestCommonPath/target/classes --sourcefiles $jacocoTestJava8Path/src/main/java --sourcefiles $jacocoTestCommonPath/src/main/java --csv $jacocoHtmlReportPath/$batchId.csv ##--caseId $caseId



