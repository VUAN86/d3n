PID_EVENT_SERVICE=`cat ./tempdir_event_service/event-service.pid`
kill -9 $PID_EVENT_SERVICE
PID_RGISTRY_SERVICE=`cat ./tempdir_service_registry/registry-service.pid`
kill -9 $PID_RGISTRY_SERVICE
PID_WORKFLOW_SERVICE=`cat ./tempdir_workflow_service/workflow-service.pid`
kill -9 $PID_WORKFLOW_SERVICE
rm -rf tempdir_service_registry
rm -rf tempdir_event_service
rm -rf tempdir_workflow_service