FROM tomcat

ADD target/proxy-sample.war /usr/local/tomcat/webapps/

EXPOSE 8080

CMD ["catalina.sh", "run"]
