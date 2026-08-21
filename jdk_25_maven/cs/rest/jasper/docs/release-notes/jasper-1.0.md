# v1.0 Release Notes

## Definitions

Most of the interface will be familiar to anyone who has used reddit, but how it actually works is
quite different.

**Refs:** In Jasper, almost everything is either a ref or a tag. This text post you are reading is a
ref. Link posts are refs, and so are comments. Refs can have sources added, which can be used to form
a graph. To maintain a directed graph, all sources must have a prior publication
date. See this [cheatsheet](https://jfcere.github.io/ngx-markdown/cheat-sheet) for what markdown
is allowed.

**Tags:** There are three kinds of tags, each defined by their own semantics: Public, protected, and
private tags. Private tags start with an underscore `_`, protected tags start with a plus sign `+`, and public tags have no prefix at all. Except for their prefixes, tags must only contain lowercase letters and forward slahses "/". Private tags may only be seen or used if you have access. Protected tags may only be used if you have access. Public tags are... totally public.

**Users:** Users are special tags that are either protected or private. They are all prefixed with
"+user/" or "_user/". Public users are disallowed since it would be possible to spoof comments. Each
user has a list of read-access and write-access tags. Any tag in your read access list gives you
access to read any ref tagged by it. Similarly for write. Protected tags are special in that read
access to a protected tag gives you access to use that tag.

**Special Tags:** There are also three special tags.
* `public` give everyone read access
* `locked` prevents any future editing by anyone
* `internal` prevents a ref from showing up on the home page or tag page

**Roles:** There are three roles users may have that give escalating levels of access:
* User Role: Anyone who can log in gains this role. You may post new refs, and tag according to your
user permissions.
* Mod Role: You can see and edit all tags and refs on the server (except for locked refs). You can
create new Feeds and Tag Extensions. You can assign permissions to any user. You have access to the modlist.
* Admin Role: all the permissions of the Mod Role, but you can also install Plugins and Templates.

**Exts:** Tag Extensions (Exts) are additional functionalty attached to a tag. The default tag
extension allows for pinning Refs to the top of the tag page, and customizing the title and sidebar. See Templates for how tags can be further extended.

**Feeds:** Mods can create RSS feeds and assign tags for content they ingest.

**Plugins:** Admins can create Plugins that extend Jasper's functionality. Usually these also
require modifying the user interface. Plugins include things such as comment threads, direct
messages, LaTeX, crypto invoices, and QR codes.

**Templates:** Admins can create templates that change how Exts work based on their tag prefix.
`user/` is a template for a User Ext that allows users to subscribe to what tags are visible on
their home page. Another template is `queue/` which allows creating work queues and managing
invoices.

## Work in Progress

**Queues:** Work queues need some work to be more user friendly. Feedback welcome.

**Graph:** The graph plugin lets you visualize the connections between Refs in a graph format.
Currently in a rough state, feedback welcome.

**User Management:** Will add SCIM integration to manage users in any SCIM compatible auth server.

**Remote Origins:** For now, Jasper only works as a single instance.

**Theming:** Allowing custom theming is still in progress. Currently there is a dark theme and a
light theme, but there just uses whichever is default for your system. I will add a toggle button
in a few days.
