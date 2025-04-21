package custom.senior.apuracao;

import java.util.List;

import org.joda.time.LocalDate;

import com.senior.rh.ponto.colaborador.HistoricoAfastamento;
import com.senior.rule.Rule;

@Rule(description = "Ajusta as situações apuradas")
public class RegraApuracao extends Apuracao {
	public void execute() {
		ContextoApuracao apuracao = getContainer().getContextoApuracao();
		LocalDate datPro = apuracao.getData();
		
		// - Marcações Impares - //
		int qtdMarcacoes = apuracao.getQtdMarcacoesRealizadas(true);
		if ((qtdMarcacoes == 3) || (qtdMarcacoes == 5) || (qtdMarcacoes == 7)) {
			apuracao.setHorSit(999, 600);
			apuracao.zeraHorasSituacao(001, 011, 015, 065, 101, 102, 103, 104, 105, 106, 301, 302, 303, 304, 305, 306,
					401, 450);
		} else {
			List<HistoricoAfastamento> afast = apuracao.getHistoricosAfastamento();

			for (int i = 0; i < afast.size(); i++) {
				int nSitAfa = afast.get(i).getSitAfa();
				int nHorAfa = afast.get(i).getHorAfa();
				int nHorTer = afast.get(i).getHorTer();
				if (afast.get(i).getDatAfa() == datPro) {
					if (afast.get(i).getSitAfa() == 15 || afast.get(i).getSitAfa() == 115
							|| afast.get(i).getSitAfa() == 116) {
						if (afast.get(i).getHorAfa() > 0) {
							apuracao.setHorSit(nSitAfa, nHorTer - nHorAfa);
							apuracao.zeraHorasSituacao(450);
						} else {
							apuracao.setHorSit(nSitAfa, apuracao.getHorSit(450));
							apuracao.zeraHorasSituacao(450);
						}
					}
				}
			}
		}

		// - Adicional Noturno - //
		/*
		 * if (apuracao.getColaborador().getNumCad() == 0){
		 * apuracao.setHorSit(2, 1); IEntitySession entytiSession =
		 * EntitySessionFactory.newSession(); //MappedParamProvider param1 = new
		 * MappedParamProvider(); ICursor<USU_ER010SIT> cur_USU_R010Sit =
		 * entytiSession.newCursor(USU_ER010SIT.class);
		 * //cur_USU_R010Sit.addFilter(" USU_AdcNot = 'S' ", param1);
		 * cur_USU_R010Sit.open(); try { USU_ER010SIT iR010Sit =
		 * cur_USU_R010Sit.newBuffer(); while (cur_USU_R010Sit.next()) {
		 * apuracao.setHorSit(2, 2); cur_USU_R010Sit.read(iR010Sit); char c =
		 * iR010Sit.getUSU_AdcNot(); if (c == 'S'){ apuracao.setHorSit(2, 3); if
		 * (apuracao.getHorSit(iR010Sit.getCodSit()) > 0)
		 * apuracao.setHorSit(100, apuracao.getHorSit(iR010Sit.getCodSit())); }
		 * } } finally { cur_USU_R010Sit.close(); entytiSession.close(); } }
		 * else
		 */
		apuracao.setHorSit(100, apuracao.getHorSit(51, 302, 304, 306, 402, 404));

		// - Total de Faltas Diárias//-@
		if (apuracao.getHorSit(450) > 0 && apuracao.getHorSit(450) <= 10) {
			apuracao.zeraHorasSituacao(450);
		}

		// - Total de Extras Diárias -//
		if (apuracao.getHorSit(401) > 0 && apuracao.getHorSit(401) < 10) {
			apuracao.zeraHorasSituacao(401);
		}

		// - Total de Extras Diárias -//
		if (apuracao.getHorSit(403) > 0 && apuracao.getHorSit(403) <= 10) {
			apuracao.zeraHorasSituacao(403);
		}

		// - Total de Extras Diárias -//
		if (apuracao.getHorSit(405) > 0 && apuracao.getHorSit(405) <= 10) {
			apuracao.zeraHorasSituacao(405);
		}

		/*
		 * if ((apuracao.getHistoricoEscala().getHorDsr() == 528) &&
		 * (apuracao.getHorSit(16, 66, 301, 302, 303, 304, 305, 306, 401, 402,
		 * 403, 404, 405, 450) > 72) && (1==2)) { try {
		 * apuracao.criarAlerta(15); } catch (ApuracaoException e) { } }
		 */

		if ((apuracao.getHorario().getCodigo() == 147) && (apuracao.getHorSit(103) > 0) && (apuracao.getHorSit(103) <= 10)) {
			if (apuracao.getHorSit(51) > 0) {
				apuracao.setHorSit(51, apuracao.getHorSit(51, 103));
				apuracao.zeraHorasSituacao(103);
			} else {
				apuracao.setHorSit(1, apuracao.getHorSit(1, 103));
				apuracao.zeraHorasSituacao(103);
			}

		}
	}
}
