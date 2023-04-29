---
layout: default
---
# Databases
The `Databases worksheet` is accessed via the `Show` menu. Just select **Databases** from it.
Here's what it looks like (Click on the image to make it bigger):
[![databases](images/databases.png)](images/databases.png)
A database is where all the accounts, budget, transactions etc. are stored. For those
technically inclined - Countary uses SQLite databases to store all its data.

On this worksheet you can see where the current database is stored. From here you can do 
a number of things:
## 1. Open the Demo database
Just click on the **Demo database...** button. This will cause a brand new temporary
database to be created. You will see its path in the field that displays the current
database path. Once you are done with the Demo database you can just close and open 
Countary and it will open your last normal database again. You can make changes to things
in the Demo database, but all changes will be lost once you close Countary. Unless you do
one of two things: 1) Copy the database file somewhere else where you can still access it,
or 2) back it up.
## Open an existing database
You can click the **Open database...** button to open an existing Countary database 
somewhere on your machine's hard disk. Use the `File-Open` dialog that pops up to locate
the database that you wish to open. Maybe you upgraded to a new computer, and you copied
your database from your old machine to the new one. Whatever your reason for opening another 
database. Countary will remember the last database you opened and will open that 
one when you open Countary again.
## Create a new database
This is something you would've done at least once when you started using Countary, but nothing
stops you from creating more databases. Click on **Create database...** to create a new one.
Use the `File-Open` dialog that pops up to provide the name and location of the new database.
The new database will be empty, but will be ready
with all the tables and other metadata that Countary needs. Countary will remember the last
database you opened or created and open it when you open Countary again.
## Make backups
Having backups of your data is never a bad idea. Just click on **Backup database...** and
use the `File-Open` dialog that pops up to locate and name the backup file.

Personally I back up my database at least twice a month - once before I start catching up on all my transactions and again once I'm
all caught up and finished with the month's transactions. Remember to move the backup to
an external drive or thumb drive, or make sure to include it in the scheduled backup of 
your machine. It won't do for you to lose everything including your backups in the event 
of a disaster. It is perhaps useful to know that a backup file is actually exactly the 
same as a normal database file - perhaps just named differently.
## Restore from a backup
A backup is only useful if you have it when you need it. Click on **Restore database...**
and use the `File-Open` dialog to find the backup that you wish to restore. *Be aware that 
this will overwrite your current open database, so please make sure this is what you want.*
## Rebuilding of virtual tables
Countary uses the Virtual Table functionality of SQLite to provide full-text search
capability. It may happen that those tables become corrupt. I've only observed this happen
once. If you get an error while searching for something and the error message advises
you to rebuild the virtual tables then clicking on the **Rebuild virtual tables...** button
will do just that. The procedure initiated by this button click will check the integrity
of the virtual tables before attempting to rebuild them, so if the tables are fine, this
will do nothing.

Back to [Home](index.markdown)
