FROM tomcat

ADD target/acrolinx-proxy-sample.war /usr/local/tomcat/webapps/

EXPOSE 8080

CMD ["catalina.sh", "run"]
