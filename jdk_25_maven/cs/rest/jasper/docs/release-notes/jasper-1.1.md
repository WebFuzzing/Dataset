# v1.1 Release Notes

This release of Jasper has built on the previous release, adding the Editor role and modified
how the Tag Based Access Control (TBAC) works.

**Users:**
* In addition to the list of read-access and write-access tags, there is now list of tag-specific
read/write access. These only let you query and see that a tag exists (read) and allow modifying the
tag's Ext (write).
* You can now add a bookmark to a Query without adding it to your home page.

**Roles:** There are four roles users may have that give escalating levels of access:
* User Role: Anyone who can log in gains this role. You may post new refs, and tag according to your
user permissions.
* Editor Role: You can tag any visible ref with any public tag. You may edit the Ext for any public
tag. You may create new Feeds.
* Mod Role: You can see and edit all tags and refs on the server (except for locked refs). You can
create new Feeds and Tag Extensions. You can assign permissions to any user. You have access to the
modlist.
* Admin Role: all the permissions of the Mod Role, but you can also install Plugins and Templates.

**Editing**: There are some new shortcuts and embeds available to make editing easier.
* Any links where the text for the link is a number or a number in square brackets<sup>[2]</sup>
will be automaticly added to the Ref's sources. Similarly with alternate URLs when the text is
like<sup>[alt1]</sup>. When using
[reference style links](https://jfcere.github.io/ngx-markdown/cheat-sheet#links) the reference name
must be a number (or alt#).
* Public tags starting with a hash sign will be added to the Ref's tags like `#jasper`. This will
also render a clickable link to the tag page.
* User tags will notify the user when this post is submitted or replied to. This will also render a
clickable link to the user tag page. Like so: +user/chris
* Using wiki-style links like `[[Jasper]]` will link to the wiki page of that name like
* All editors now have live markdown preview


**Embedding**: You can now embed any Ref, Query, audio, video, or image in another Ref.
* Drag any image or video to resize it.
* Any links to Refs, queries, audio, video, image will display an embed toggle button to expand
them
* A link with the special text "ref" will just show the Ref and not show the link, like so
`[ref](/ref/url)`
* Links to embeddable content also work, does not have to be a Ref, like so
`[video](https://i.imgur.com/uYMg9nK.mp4)`
* A link with the special text "embed" will just embed the contents of the Ref, like so
`[embed](/ref/url)`
* A link with the special text "query" will just embed the results of a tag query, like so
`[query](jasper)`

**Curity Integration**: Jasper is now integrated with Curity using SCIM. You can change your
password (if you've logged in within the last 15 minutes) on the settings page `/settings/password`.
Admins can create, delete, activate, deactivate, set role, and change the password of all users.
