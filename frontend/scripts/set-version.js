const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Get version from package.json
const packageJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../package.json'), 'utf8'));
const version = packageJson.version;

// Get git commit hash
let gitCommit = '';
try {
  gitCommit = execSync('git rev-parse --short HEAD').toString().trim();
} catch (e) {
  console.warn('Unable to get git commit hash:', e.message);
}

// Create version string with commit hash if available
const versionString = gitCommit ? `${version}-${gitCommit}` : version;

// Get current date
const buildDate = new Date().toISOString().split('T')[0];

// Create or update .env file
const envPath = path.resolve(__dirname, '../.env');
let envContent = '';

// Read existing .env if it exists
if (fs.existsSync(envPath)) {
  envContent = fs.readFileSync(envPath, 'utf8');
}

// Update or add version and build date
const versionRegex = /^VITE_APP_VERSION=.*/m;
const buildDateRegex = /^VITE_APP_BUILD_DATE=.*/m;

if (versionRegex.test(envContent)) {
  envContent = envContent.replace(versionRegex, `VITE_APP_VERSION=${versionString}`);
} else {
  envContent += `\nVITE_APP_VERSION=${versionString}`;
}

if (buildDateRegex.test(envContent)) {
  envContent = envContent.replace(buildDateRegex, `VITE_APP_BUILD_DATE=${buildDate}`);
} else {
  envContent += `\nVITE_APP_BUILD_DATE=${buildDate}`;
}

// Write updated .env file
fs.writeFileSync(envPath, envContent.trim());

console.log(`Version set to ${versionString}, build date set to ${buildDate}`); 