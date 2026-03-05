package io.github.gvergine.ifsx.web;

import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;
import io.github.gvergine.ifsx.core.model.IfsImage;
import io.github.gvergine.ifsx.core.parser.DumpIfsParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/api/inspect")
@MultipartConfig(maxFileSize = 256 * 1024 * 1024)
public class IfsxServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Part filePart = req.getPart("image");
        if (filePart == null) { resp.sendError(400, "Missing image upload"); return; }
        Path tmp = Files.createTempFile("ifsx-", ".ifs");
        try {
            filePart.write(tmp.toString());
            SdpToolExecutor exec = new SdpToolExecutor();
            String verbose = exec.runDumpIfsVerbose(tmp);
            IfsImage image = new DumpIfsParser().parse(verbose);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            writeJson(resp.getWriter(), image);
        } finally { Files.deleteIfExists(tmp); }
    }

    private void writeJson(PrintWriter pw, IfsImage image) {
        pw.println("{");
        pw.printf("  \"entryCount\": %d,%n", image.getEntries().size());
        if (image.getImageHeader() != null)
            pw.printf("  \"mountpoint\": \"%s\",%n",
                image.getImageHeader().getMountpoint());
        pw.println("  \"entries\": [");
        var entries = image.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            pw.printf("    {\"type\": \"%s\", \"path\": \"%s\"}%s%n",
                e.getType(), e.getPath(),
                (i < entries.size() - 1) ? "," : "");
        }
        pw.println("  ]"); pw.println("}");
    }
}
