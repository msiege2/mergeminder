# MergeMinder
[![Build Status](https://travis-ci.com/msiege2/mergeminder.svg?branch=master)](https://travis-ci.com/msiege2)
![Dev Version](https://pom-version-badge.glitch.me/msiege2/mergeminder)
[![HitCount](http://hits.dwyl.io/msiege2/mergeminder.svg)](http://hits.dwyl.io/msiege2/mergeminder)

MergeMinder is a service that scrapes Gitlab projects for open merge requests and sends increasingly hostile Slack messages to users as they sit open.
## Getting Started
#### Prerequisites:

1. MergeMinder uses MySQL as it's RDBMS.  It requires its schema is set up before usage.  There is not currently support for creating this, so if you don't have it, you are out of luck right now.  Techincally, MergeMinder uses Spring Data so the could could be set to create the schema on its own, however, that is not covered in these instructions. 

2. MergeMinder requires Gitlab integration.  In order to set this up, you will need to have a Gitlab user's API token with API permissions assigned to it.

3. MergeMinder requires Slack integration.  Make sure you have created a MergeMinder app in your Slack workspace and have the bot token for that app available for MergeMinder.

   

### Setup:

In order to run MergeMinder, you will need to provide a series of properties to the application.

#### Required Parameters
 
| Property Name                  | Descrption     | Example             |
| :---                           |          :---  | :---                |
| `mm.gitlab.url`                | Gitlab URL | `https://git.myrepo.com` |
| `mm.gitlab.accesstoken`        | Gitlab API access token | `ABCDEFG` |
| `mm.slack.bottoken`            | API token for MergeMinder Slack bot | `xoxb-abcdefg12345678` |
| `spring.datasource.url`        | JDBC URL for the MergeMinder DB | `jdbc:mysql://db.com:3306/MergeMinder` |
| `spring.datasource.username`   | Username for the MergeMinder DB | `myUser` |
| `spring.datasource.password`   | Password for the MergeMinder DB | `myPassword` | 
#### Optional Parameters

| Property Name                  | Descrption     | Example             |
| :---                           |          :---  | :---                |
| `mm.slack.notificationchannel` | Notification channel for MergeMinder events | `#mergeminder` | 
| `mm.slack.notifyusers`         | Determines if user should be sent DMs or not (default: false)  | `true` or `false`|
| `mm.emaildomains`              | Comma separated list of domains to use to look up gitlab users in Slack | `mydomain.com` |
| `mm.adminemails`               | Comma separated list of Slack user's emails who are allowed to use admin functionality | `user@domain.com` |
| `mm.slack.notifyusers`         | Determines if user should be sent DMs or not (default: false)  | `true` or `false`|
| `mm.emaildomains`              | Comma separated list of domains to use to look up gitlab users in Slack | `mydomain.com` |
| `mm.adminemails`               | Comma separated list of Slack user's emails who are allowed to use admin functionality | `user@domain.com` |
| `mm.gitlab.ignoredByLabels`    | Comma separated list of merge request labels that will skip reminding assignee | `dependency-update` |

## Running MergeMinder:

The easiest way to run MergeMinder is by running the pre-built docker container.  This is available through the Docker Hub registry.  If you have docker running on your host, simply run: 

    `docker pull msiege2/mergeminder`

This will pull the latest docker image from the container registry.  After pulling the latest image, run with with:

    `docker run msiege2/mergeminder`

This will start the container.  In order to pass parameters into the application, it is recommended to pass them as environment parameters.  Pass them using the `-e` parameter on the docker command line.  Because MergeMinder uses Spring Boot, it recognizes property names using the relaxed binding rules defined in the Spring Boot documentation at [Spring Boot External Config Doc](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-relaxed-binding).  For example:

    `docker run -e 'MM_GITLAB_URL=http://my.gitlab.com' msiege2/mergeminder`
