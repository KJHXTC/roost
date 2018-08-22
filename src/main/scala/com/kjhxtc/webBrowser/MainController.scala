package com.kjhxtc.webBrowser

import com.jfinal.core.Controller


class MainController extends Controller {
  def index(): Unit = {
    render("edge.html")
  }
}
