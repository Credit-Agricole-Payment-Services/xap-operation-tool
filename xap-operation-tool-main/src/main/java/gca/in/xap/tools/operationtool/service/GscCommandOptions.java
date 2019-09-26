package gca.in.xap.tools.operationtool.service;

import lombok.Data;

import java.util.List;

@Data
public class GscCommandOptions {
	private List<String> environments;
	private List<String> vmInputs;
}
