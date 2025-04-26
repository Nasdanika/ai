To generate a documentation website from your Drawio diagram and publish it to GitHub Pages, you can utilize the Nasdanika CLI (`nsd`). The process involves converting your Drawio diagram into an HTML application model and then generating a documentation site from it. Here’s a step-by-step guide:

### Step 1: Install Nasdanika CLI
Ensure that you have Nasdanika CLI installed. The CLI requires Java 17 or above.

### Step 2: Prepare Your Drawio Diagram
Ensure that your Drawio diagram is ready. The diagrams can contain nodes, connections, and additional properties such as documentation, icons, and titles that will be used in the generated site.

### Step 3: Generate HTML Application Model
Convert your Drawio diagram into an HTML application model using the `nsd drawio html-app` command. Here is an example command:

```shell
nsd drawio html-app -r root-action.yml --add-to-root
```

- `-r root-action.yml` specifies the root action configuration.
- `--add-to-root` adds the generated labels to the root action.

### Step 4: Generate HTML Site
Use the `site` command to generate the HTML documentation site. Below is an example:

```shell
nsd drawio html-app site -r=-1 -F page-template.yml docs
```

- `-r=-1` specifies that the generation should not fail on page errors.
- `-F page-template.yml` specifies the page template to use.
- `docs` is the output directory where the generated site will be stored.

### Step 5: Publish to GitHub Pages
To publish the generated documentation to GitHub Pages:

1. **Commit your changes**: Add the generated `docs` folder to your repository.
   ```shell
   git add docs
   git commit -m "Add generated documentation site"
   ```

2. **Push to GitHub**: Push your changes to GitHub.
   ```shell
   git push origin main
   ```

3. **Configure GitHub Pages**:
   - Go to your GitHub repository settings.
   - Navigate to the "Pages" section.
   - Set the source to the `main` branch and the `/docs` folder.

Now, your documentation should be available as a GitHub Pages site.

### References:
- [Nasdanika CLI drawio html-app Command](https://docs.nasdanika.org/nsd-cli/nsd/drawio/html-app/index.html)
- [Nasdanika CLI drawio html-app site Command](https://docs.nasdanika.org/nsd-cli/nsd/drawio/html-app/site/index.html)
- [Nasdanika CLI site Command](https://docs.nasdanika.org/nsd-cli/nsd/app/index.html)

By following the above steps, you should be able to transform your Drawio diagram into a nice-looking HTML documentation site and publish it on GitHub Pages.
