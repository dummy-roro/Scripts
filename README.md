# 🚀 DevOps Environment Setup

This script sets up a full DevOps environment with:
- Java, Jenkins, Docker, SonarQube
- AWS CLI, Terraform, Helm
- Kubernetes (kubeadm), ArgoCD
- Security tools: Trivy
- Optional: Minikube

---

## 🛠️ Run This Script

```bash
chmod +x setup.sh
./setup.sh

☕ Java & Jenkins

sudo apt update -y
sudo apt install openjdk-17-jre openjdk-17-jdk -y
java --version
# Jenkins installation
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update -y
sudo apt-get install jenkins -y
