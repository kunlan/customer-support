package com.jint;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(
        name = "ticketServlet",
        urlPatterns = {"/tickets"},
        loadOnStartup = 1
)
@MultipartConfig(
        fileSizeThreshold = 5_242_880, //5MB
        maxFileSize = 20_971_520L, //20MB
        maxRequestSize = 41_943_040L //40MB
)
public class TicketServlet extends HttpServlet
{
    private volatile int TICKET_ID_SEQUENCE = 1;

    private Map<Integer, Ticket> ticketDatabase = new LinkedHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        String action = req.getParameter("action");
        if(action == null)
            action = "list";
        switch(action)
        {
            case "create":
                this.showTicketForm(resp);
                break;
            case "view":
                this.viewTicket(req, resp);
                break;
            case "download":
                this.downloadAttachment(req, resp);
                break;
            case "list":
            default:
                this.listTickets(resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        String action = req.getParameter("action");
        if(action == null)
            action = "list";
        switch(action)
        {
            case "create":
                this.createTicket(req, resp);
                break;
            case "list":
            default:
                resp.sendRedirect("tickets");
                break;
        }
    }

    private void showTicketForm(HttpServletResponse resp)
            throws ServletException, IOException
    {
        PrintWriter writer = this.writeHeader(resp);

        writer.append("<h2>Create a Ticket</h2>\r\n");
        writer.append("<form method=\"POST\" action=\"tickets\" ")
              .append("enctype=\"multipart/form-data\">\r\n");
        writer.append("<input type=\"hidden\" name=\"action\" ")
              .append("value=\"create\"/>\r\n");
        writer.append("Your Name<br/>\r\n");
        writer.append("<input type=\"text\" name=\"customerName\"/><br/><br/>\r\n");
        writer.append("Subject<br/>\r\n");
        writer.append("<input type=\"text\" name=\"subject\"/><br/><br/>\r\n");
        writer.append("Body<br/>\r\n");
        writer.append("<textarea name=\"body\" rows=\"5\" cols=\"30\">")
              .append("</textarea><br/><br/>\r\n");
        writer.append("<b>Attachments</b><br/>\r\n");
        writer.append("<input type=\"file\" name=\"file1\"/><br/><br/>\r\n");
        writer.append("<input type=\"submit\" value=\"Submit\"/>\r\n");
        writer.append("</form>\r\n");

        this.writeFooter(writer);
    }

    private void viewTicket(HttpServletRequest req,
                            HttpServletResponse resp)
            throws ServletException, IOException
    {
        String idString = req.getParameter("ticketId");
        Ticket ticket = this.getTicket(idString, resp);
        if(ticket == null)
            return;

        PrintWriter writer = this.writeHeader(resp);

        writer.append("<h2>Ticket #").append(idString)
              .append(": ").append(ticket.getSubject()).append("</h2>\r\n");
        writer.append("<i>Customer Name - ").append(ticket.getCustomerName())
              .append("</i><br/><br/>\r\n");
        writer.append(ticket.getBody()).append("<br/><br/>\r\n");

        if(ticket.getNumberOfAttachments() > 0)
        {
            writer.append("Attachments: ");
            int i = 0;
            for(Attachment attachment : ticket.getAttachments())
            {
                if(i++ > 0)
                    writer.append(", ");
                writer.append("<a href=\"tickets?action=download&ticketId=")
                      .append(idString).append("&attachment=")
                      .append(attachment.getName()).append("\">")
                      .append(attachment.getName()).append("</a>");
            }
            writer.append("<br/><br/>\r\n");
        }

        writer.append("<a href=\"tickets\">Return to list tickets</a>\r\n");

        this.writeFooter(writer);
    }

    private void downloadAttachment(HttpServletRequest req,
                                    HttpServletResponse resp)
            throws ServletException, IOException
    {
        String idString = req.getParameter("ticketId");
        Ticket ticket = this.getTicket(idString, resp);
        if(ticket == null)
            return;

        String name = req.getParameter("attachment");
        if(name == null)
        {
            resp.sendRedirect("tickets?action=view&ticketId=" + idString);
            return;
        }

        Attachment attachment = ticket.getAttachment(name);
        if(attachment == null)
        {
            resp.sendRedirect("tickets?action=view&ticketId=" + idString);
            return;
        }

        resp.setHeader("Content-Disposition",
                "attachment; filename=" + attachment.getName());
        resp.setContentType("application/octet-stream");

        ServletOutputStream stream = resp.getOutputStream();
        stream.write(attachment.getContents());
    }

    private void listTickets(HttpServletResponse resp)
            throws ServletException, IOException
    {
        PrintWriter writer = this.writeHeader(resp);

        writer.append("<h2>Tickets</h2>\r\n");
        writer.append("<a href=\"tickets?action=create\">Create Ticket")
              .append("</a><br/><br/>\r\n");

        if(this.ticketDatabase.size() == 0)
            writer.append("<i>There are no tickets in the system.</i>\r\n");
        else
        {
            for(int id : this.ticketDatabase.keySet())
            {
                String idString = Integer.toString(id);
                Ticket ticket = this.ticketDatabase.get(id);
                writer.append("Ticket #").append(idString)
                      .append(": <a href=\"tickets?action=view&ticketId=")
                      .append(idString).append("\">").append(ticket.getSubject())
                      .append("</a> (customer: ").append(ticket.getCustomerName())
                      .append(")<br/>\r\n");
            }
        }

        this.writeFooter(writer);
    }

    private void createTicket(HttpServletRequest req,
                              HttpServletResponse resp)
            throws ServletException, IOException
    {
        Ticket ticket = new Ticket();
        ticket.setCustomerName(req.getParameter("customerName"));
        ticket.setSubject(req.getParameter("subject"));
        ticket.setBody(req.getParameter("body"));

        Part filePart = req.getPart("file1");
        if(filePart != null && filePart.getSize() > 0)
        {
            Attachment attachment = this.processAttachment(filePart);
            if(attachment != null)
                ticket.addAttachment(attachment);
        }

        int id;
        synchronized(this)
        {
            id = this.TICKET_ID_SEQUENCE++;
            this.ticketDatabase.put(id, ticket);
        }

        resp.sendRedirect("tickets?action=view&ticketId=" + id);
    }

    private Attachment processAttachment(Part filePart)
            throws IOException
    {
        InputStream inputStream = filePart.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int read;
        final byte[] bytes = new byte[1024];

        while((read = inputStream.read(bytes)) != -1)
        {
            outputStream.write(bytes, 0, read);
        }

        Attachment attachment = new Attachment();
        attachment.setName(filePart.getSubmittedFileName());
        attachment.setContents(outputStream.toByteArray());

        return attachment;
    }

    private Ticket getTicket(String idString, HttpServletResponse resp)
            throws ServletException, IOException
    {
        if(idString == null || idString.length() == 0)
        {
            resp.sendRedirect("tickets");
            return null;
        }

        try
        {
            Ticket ticket = this.ticketDatabase.get(Integer.parseInt(idString));
            if(ticket == null)
            {
                resp.sendRedirect("tickets");
                return null;
            }
            return ticket;
        }
        catch(Exception e)
        {
            resp.sendRedirect("tickets");
            return null;
        }
    }

    private PrintWriter writeHeader(HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

		PrintWriter writer = resp.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
              .append("<html>\r\n")
              .append("    <head>\r\n")
              .append("        <title>Customer Support</title>\r\n")
              .append("    </head>\r\n")
              .append("    <body>\r\n");

        return writer;
    }

    private void writeFooter(PrintWriter writer)
    {
        writer.append("    </body>\r\n").append("</html>\r\n");
    }
}
