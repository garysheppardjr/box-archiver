package io.github.garysheppardjr.box.archiver;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ArchiveServlet extends HttpServlet {

  private static final String STATE_OK = "ok";
  private static final Pattern PATTERN_PARAM_CODE_OR_STATE = Pattern.compile("(?:code|state)=[^&]*");

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final String boxCode = request.getParameter("code");
    final boolean preserve = Boolean.parseBoolean(request.getParameter("preserve"));
    long days;
    try {
      days = Long.parseLong(request.getParameter("minagedays"));
    } catch (Throwable t) {
      days  = 365 * 6000;
    }
    final long minAgeDays = days;

    BoxAPIConnection boxApi = null;
    if (null != boxCode && STATE_OK.equals(request.getParameter("state"))) {
      try {
        boxApi = new BoxAPIConnection(
            getServletConfig().getInitParameter("boxClientId"),
            getServletConfig().getInitParameter("boxClientSecret"),
            boxCode
        );
      } catch (BoxAPIException ex) {
        Logger.getLogger(ArchiveServlet.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    if (null != boxApi) {
      BoxFolder targetFolder = new BoxFolder(
          boxApi,
          getServletConfig().getInitParameter("boxTargetFolderId")
      );
      List<Path> archivedFiles = archiveFiles(
          Paths.get(getServletConfig().getInitParameter("directoryToArchive")),
          targetFolder,
          minAgeDays * 24 * 60 * 60 * 1000,
          preserve
      );
      response.setContentType("text/html;charset=UTF-8");
      try (PrintWriter out = response.getWriter()) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet ArchiveServlet</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Archived Files</h1>");
        
        out.println("<ul>");
        archivedFiles.stream().forEachOrdered(path -> {
          out.println("<li>" + path.getFileName().toString() + "</li>");
        });
        out.println("</ul>");
        
        out.println("</body>");
        out.println("</html>");
      }
    } else {
      doLogin(request, response);
    }
  }

  private void doLogin(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    StringBuffer redirectUri = request.getRequestURL();
    String queryString = request.getQueryString();
    if (null != queryString) {
      queryString = queryString.trim();
      if (!queryString.isEmpty()) {
        // Remove code and state parameters because we're logged out
        queryString = PATTERN_PARAM_CODE_OR_STATE.matcher(queryString).replaceAll("");
        redirectUri.append("?").append(queryString);
      }
    }
    try {
      URL authorizationUrl = BoxAPIConnection.getAuthorizationURL(
          getServletConfig().getInitParameter("boxClientId"),
          new URI(redirectUri.toString()),
          STATE_OK,
          null
      );
      response.sendRedirect(authorizationUrl.toString());
    } catch (URISyntaxException e) {
      response.sendRedirect("error.jsp");
    }
  }

  private List<Path> archiveFiles(
      final Path directory,
      final BoxFolder boxFolder,
      final long minAgeMillis
  ) throws IOException {
    return archiveFiles(directory, boxFolder, minAgeMillis, false);
  }

  private List<Path> archiveFiles(
      final Path directory,
      final BoxFolder boxFolder,
      final long minAgeMillis,
      final boolean preserve
  ) throws IOException {
    final ArrayList<Path> archivedFiles = new ArrayList<>();
    
    try {
      boxFolder.canUpload(UUID.randomUUID().toString(), 0);
    } catch (Throwable t) {
      Logger.getLogger(ArchiveServlet.class.getName()).log(Level.SEVERE, "Cannot upload to folder " + boxFolder.getID(), t);
      return archivedFiles;
    }
    
    final long largestTimeToArchive = System.currentTimeMillis() - minAgeMillis;
    Files.newDirectoryStream(directory, entry -> {
      try {
        BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class);
        return Files.isRegularFile(entry)
            && attributes.lastModifiedTime().toMillis() <= largestTimeToArchive;
      } catch (Throwable t) {
        return false;
      }
    }).forEach(path -> {
      try {
        BoxFile.Info uploadFile = null;
        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
          try {
            uploadFile = boxFolder.uploadFile(in, path.getFileName().toString());
          } catch (Throwable t) {
            Logger.getLogger(ArchiveServlet.class.getName()).log(Level.SEVERE, null, t);
          }
        }
        if (null != uploadFile && !preserve) {
          Files.deleteIfExists(path);
          archivedFiles.add(path);
        }
      } catch (IOException ex) {
        Logger.getLogger(ArchiveServlet.class.getName()).log(Level.SEVERE, null, ex);
      }
    });
    return archivedFiles;
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Archives files on disk to Box";
  }// </editor-fold>

}
