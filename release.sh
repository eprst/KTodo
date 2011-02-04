#!/usr/bin/expect -f

set timeout -1

set fp [open "keypass" r]
set pass [read $fp]
close $fp

spawn ant release
expect {
    "Please enter"    {send -- "$pass\n"; exp_continue}
}