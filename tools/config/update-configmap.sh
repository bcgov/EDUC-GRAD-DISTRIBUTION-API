###########################################################
#ENV VARS
###########################################################
envValue=$1
APP_NAME=$2
GRAD_NAMESPACE=$3
COMMON_NAMESPACE=$4
BUSINESS_NAMESPACE=$5
SPLUNK_TOKEN=$6
APP_LOG_LEVEL=$7
BCMAIL_LOCATION=$8

SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    info
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
   Buffer_Max_Size 1MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  absolutely_nothing_bud
   Log_Level    off
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"
###########################################################
#Setup for config-maps
###########################################################
echo Creating config map "$APP_NAME"-config-map
oc create -n "$BUSINESS_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map \
 --from-literal=APP_LOG_LEVEL="$APP_LOG_LEVEL" \
 --from-literal=ENABLE_SPLUNK_LOG_HELPER="true" \
 --from-literal=GRAD_REPORT_API="http://educ-grad-report-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=CLEAN_TMP_CACHE_INTERVAL="5" \
 --from-literal=GRAD_TRAX_API="http://educ-grad-trax-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=EDUC_SCHOOL_API="http://school-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=GRAD_BATCH_API="http://educ-grad-batch-graduation-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=GRAD_STUDENT_API="http://educ-grad-student-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=CLEAN_TMP_CACHE_CRON="0 0 */12 * * *" \
 --from-literal=CLEAN_TEMP_CACHE_FILTER="{hsperf*,undertow*,.nfs*,.java*,.nfs*,Batch,FTP,PAPER}" \
 --from-literal=GRAD_GRADUATION_REPORT_API="http://educ-grad-graduation-report-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=BCMAIL_SFTP_HOST="SOLIMAR1.qp.gov.bc.ca" \
 --from-literal=BCMAIL_SFTP_USER="edgrad_sftp" \
 --from-literal=GRAD_GRADUATION_API="http://educ-grad-graduation-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --from-literal=KEYCLOAK_TOKEN_URL="https://soam-$envValue.apps.silver.devops.gov.bc.ca/" \
 --from-literal=BCMAIL_LOCATION="$BCMAIL_LOCATION" \
 --from-literal=ENABLE_COMPRESSION="true" \
 --dry-run=client -o yaml | oc apply -f -

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$BUSINESS_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map \
  --from-literal=fluent-bit.conf="$FLB_CONFIG" \
  --from-literal=parsers.conf="$PARSER_CONFIG" \
  --dry-run=client -o yaml | oc apply -f -
