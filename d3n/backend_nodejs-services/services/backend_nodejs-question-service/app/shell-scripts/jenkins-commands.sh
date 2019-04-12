rm -rf node_modules
npm install
./app/shell-scripts/start-java-registry-and-event-service.sh
npm test
./app/shell-scripts/stop-java-registry-and-event-service.sh
