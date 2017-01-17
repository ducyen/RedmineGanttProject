echo off
call BuildAHouse_Common.bat
set ASSIGNEE=Duc Hoang,Another
java %JAVA_FLAGS% -jar ..\..\release\RedmineToGanttProj.jar load
pause

