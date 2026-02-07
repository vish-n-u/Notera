package com.example.devaudioreccordings.wordpress_comments

import org.wordpress.aztec.handlers.GenericBlockHandler

class GutenbergCommentHandler : GenericBlockHandler<GutenbergCommentSpan>(GutenbergCommentSpan::class.java)