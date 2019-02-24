# MergeMinder
MergeMinder is a service that scrapes Gitlab projects for open merge requests and sends increasingly hostile Slack messages to users as they sit open.
## Getting Started
#### Prerequisites:

1. MergeMinder uses MySQL as it's RDBMS.  It requires its schema is set up before usage.  There is not currently support for creating this, so if you don't have it, you are out of luck right now.  Techincally, MergeMinder uses Spring Data so the could could be set to create the schema on its own, however, that is not covered in these instructions. 

2. MergeMinder requires Gitlab integration.  In order to set this up, you will need to have a Gitlab user's API token with API permissions assigned to it.

3. MergeMinder requires Slack integration.  Make sure you have created a MergeMinder app in your Slack workspace and have the bot token for that app available for MergeMinder.

   

### Setup:

In order to run MergeMinder, you will need to provide a series of properties to the application.
